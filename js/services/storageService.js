/**
 * Persistence layer: seed loading (fetch /data/*.json with embedded fallback),
 * localStorage read/write, JSON export/import and factory reset.
 * The only module that touches localStorage. Every write is announced through syncService.
 */
import { STORAGE_KEYS, ENTITIES, DATA_FILES, SEED_DATA } from '../config.js';
import { validateBackupStructure } from '../utils/validators.js';
import { notifyChange } from './syncService.js';

export class StorageError extends Error {
  constructor(message, cause) {
    super(message);
    this.name = 'StorageError';
    this.cause = cause;
  }
}

/* ---------- Low-level access (with in-memory fallback if localStorage is blocked) ---------- */

const memoryStore = new Map();
let persistent = true;

function readRaw(key) {
  if (!persistent) return memoryStore.get(key) ?? null;
  try {
    return localStorage.getItem(key);
  } catch {
    persistent = false;
    return memoryStore.get(key) ?? null;
  }
}

function writeRaw(key, value) {
  if (!persistent) {
    memoryStore.set(key, value);
    return;
  }
  try {
    localStorage.setItem(key, value);
  } catch (error) {
    if (error?.name === 'QuotaExceededError') {
      throw new StorageError('No hay espacio suficiente en el navegador para guardar los cambios', error);
    }
    persistent = false;
    memoryStore.set(key, value);
  }
}

function removeRaw(key) {
  memoryStore.delete(key);
  try {
    localStorage.removeItem(key);
  } catch {
    /* storage blocked: memory copy already removed */
  }
}

function readEntity(entity) {
  const raw = readRaw(STORAGE_KEYS[entity]);
  if (raw === null) return null;
  try {
    const parsed = JSON.parse(raw);
    return Array.isArray(parsed) ? parsed : null;
  } catch {
    return null;
  }
}

function writeEntity(entity, items) {
  writeRaw(STORAGE_KEYS[entity], JSON.stringify(items));
}

/* ---------- Seed loading ---------- */

async function fetchJSON(url) {
  const response = await fetch(url, { cache: 'no-store' });
  if (!response.ok) throw new Error(`HTTP ${response.status} al cargar ${url}`);
  return response.json();
}

async function fetchSeedFiles() {
  const results = await Promise.all(ENTITIES.map((entity) => fetchJSON(DATA_FILES[entity])));
  return Object.fromEntries(ENTITIES.map((entity, index) => [entity, results[index]]));
}

/** Loads seed data from /data/ or, if fetch fails (e.g. file://), from config.js. */
async function loadSeedData() {
  try {
    const data = await fetchSeedFiles();
    if (validateBackupStructure(data).valid) return { data, source: 'fetch' };
    console.warn('[storageService] Seed files are invalid; using embedded seed data');
  } catch (error) {
    console.info('[storageService] Seed files unavailable; using embedded seed data.', error.message);
  }
  return { data: structuredClone(SEED_DATA), source: 'embedded' };
}

/**
 * Ensures every collection exists in localStorage. Missing or corrupt
 * collections are restored from the seed. Returns where data came from.
 */
export async function initStorage() {
  const missing = ENTITIES.filter((entity) => readEntity(entity) === null);
  if (missing.length === 0) return { source: 'storage', persistent };
  const { data, source } = await loadSeedData();
  missing.forEach((entity) => writeEntity(entity, data[entity]));
  return { source, persistent, restored: missing };
}

/* ---------- Public CRUD primitives ---------- */

/** Returns a fresh copy of a collection (safe to mutate). */
export function getCollection(entity) {
  if (!ENTITIES.includes(entity)) throw new StorageError(`Colección desconocida: ${entity}`);
  return readEntity(entity) ?? structuredClone(SEED_DATA[entity]);
}

/** Persists a whole collection and notifies every tab. */
export function saveCollection(entity, items) {
  if (!ENTITIES.includes(entity)) throw new StorageError(`Colección desconocida: ${entity}`);
  if (!Array.isArray(items)) throw new StorageError('Los datos a guardar deben ser una lista');
  writeEntity(entity, items);
  notifyChange(entity);
}

/** Unique id: crypto.randomUUID() with a Date.now() fallback. */
export function createId(prefix) {
  let unique;
  try {
    unique = crypto.randomUUID();
  } catch {
    unique = `${Date.now().toString(36)}${Math.random().toString(36).slice(2, 8)}`;
  }
  return prefix ? `${prefix}_${unique}` : unique;
}

export function isPersistent() {
  return persistent;
}

/* ---------- Backup: export / import / reset ---------- */

function todayStamp() {
  const now = new Date();
  const pad = (value) => String(value).padStart(2, '0');
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`;
}

export function buildBackup() {
  const backup = { app: 'Habitat CRM', exportedAt: new Date().toISOString() };
  ENTITIES.forEach((entity) => { backup[entity] = getCollection(entity); });
  return backup;
}

/** Downloads crm_backup_YYYY-MM-DD.json with the full current state. */
export function exportBackup() {
  const fileName = `crm_backup_${todayStamp()}.json`;
  const blob = new Blob([JSON.stringify(buildBackup(), null, 2)], { type: 'application/json' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = fileName;
  document.body.append(link);
  link.click();
  link.remove();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
  return fileName;
}

/** Writes all collections atomically: on failure the previous state is restored. */
function replaceAllCollections(data) {
  const snapshot = Object.fromEntries(ENTITIES.map((entity) => [entity, readRaw(STORAGE_KEYS[entity])]));
  try {
    ENTITIES.forEach((entity) => writeEntity(entity, data[entity]));
  } catch (error) {
    ENTITIES.forEach((entity) => {
      if (snapshot[entity] === null) removeRaw(STORAGE_KEYS[entity]);
      else writeRaw(STORAGE_KEYS[entity], snapshot[entity]);
    });
    throw error;
  }
  notifyChange('all');
}

/** Validates and applies a previously exported backup (JSON text). */
export function importBackup(jsonText) {
  let data;
  try {
    data = JSON.parse(jsonText);
  } catch {
    return { ok: false, error: 'El archivo no es un JSON válido' };
  }
  const { valid, error } = validateBackupStructure(data);
  if (!valid) return { ok: false, error };
  try {
    replaceAllCollections(data);
  } catch (writeError) {
    return { ok: false, error: writeError.message || 'No se pudo aplicar el respaldo' };
  }
  const counts = Object.fromEntries(ENTITIES.map((entity) => [entity, data[entity].length]));
  return { ok: true, error: null, counts };
}

/** Clears this app's keys and reloads the original seed data. */
export async function resetToSeed() {
  ENTITIES.forEach((entity) => removeRaw(STORAGE_KEYS[entity]));
  const { data, source } = await loadSeedData();
  replaceAllCollections(data);
  return { source };
}
