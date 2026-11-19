/**
 * Property business logic: queries, filters and CRUD with validation.
 * All persistence goes through storageService (which notifies other tabs).
 */
import { DEFAULT_CURRENCY, PROPERTY_STATUS_LABELS } from '../config.js';
import { validatePropertyData } from '../utils/validators.js';
import { parseNumber, normalizeText } from '../utils/formatters.js';
import { getCollection, saveCollection, createId } from './storageService.js';

const NUMERIC_FIELDS = ['price', 'areaM2', 'bedrooms', 'bathrooms', 'parking'];
const TEXT_FIELDS = ['title', 'type', 'operation', 'location', 'status', 'description', 'imageUrl'];

export const EMPTY_FILTERS = Object.freeze({ query: '', type: '', operation: '', status: '', maxPrice: null });

function byNewest(a, b) {
  return String(b.createdAt).localeCompare(String(a.createdAt));
}

/** All properties, newest first. */
export function getProperties() {
  return getCollection('properties').sort(byNewest);
}

export function getPropertyById(id) {
  return getCollection('properties').find((property) => property.id === id) ?? null;
}

/* ---------- Filters ---------- */

function matchesQuery(property, query) {
  if (!query) return true;
  const haystack = normalizeText(`${property.title} ${property.location}`);
  return normalizeText(query).split(/\s+/).every((word) => haystack.includes(word));
}

function matchesFilters(property, filters) {
  if (filters.type && property.type !== filters.type) return false;
  if (filters.operation && property.operation !== filters.operation) return false;
  if (filters.status && property.status !== filters.status) return false;
  if (Number.isFinite(filters.maxPrice) && filters.maxPrice > 0 && property.price > filters.maxPrice) return false;
  return matchesQuery(property, filters.query);
}

/** Pure filter: { query, type, operation, status, maxPrice } — empty values mean "any". */
export function filterProperties(properties, filters = EMPTY_FILTERS) {
  const active = { ...EMPTY_FILTERS, ...filters };
  return properties.filter((property) => matchesFilters(property, active));
}

export function hasActiveFilters(filters) {
  const active = { ...EMPTY_FILTERS, ...filters };
  return Boolean(active.query.trim() || active.type || active.operation || active.status || active.maxPrice);
}

/** Highest price, optionally for one operation — used to bound the price filter. */
export function getMaxPrice(properties, operation = '') {
  const prices = properties.filter((p) => !operation || p.operation === operation).map((p) => p.price);
  return prices.length ? Math.max(...prices) : 0;
}

/* ---------- CRUD ---------- */

/** Converts raw form values (strings) into a typed property draft. */
export function normalizePropertyInput(raw) {
  const draft = {};
  TEXT_FIELDS.forEach((field) => { draft[field] = String(raw[field] ?? '').trim(); });
  NUMERIC_FIELDS.forEach((field) => {
    const value = raw[field];
    draft[field] = value === '' || value === null || value === undefined ? NaN : parseNumber(value);
  });
  draft.currency = raw.currency || DEFAULT_CURRENCY;
  return draft;
}

/** Validates and creates a property. Returns { ok, property } or { ok: false, errors }. */
export function createProperty(rawInput, agentId) {
  const draft = normalizePropertyInput(rawInput);
  const { valid, errors } = validatePropertyData(draft);
  if (!valid) return { ok: false, errors };
  const property = { id: createId('prop'), ...draft, agentId, createdAt: new Date().toISOString() };
  saveCollection('properties', [...getCollection('properties'), property]);
  return { ok: true, property };
}

/** Validates and updates a property, keeping id, agent and creation date. */
export function updateProperty(id, rawInput) {
  const properties = getCollection('properties');
  const index = properties.findIndex((property) => property.id === id);
  if (index === -1) return { ok: false, errors: { _form: 'La propiedad ya no existe. Puede que la hayan eliminado en otra pestaña' } };
  const draft = normalizePropertyInput(rawInput);
  const { valid, errors } = validatePropertyData(draft);
  if (!valid) return { ok: false, errors };
  const current = properties[index];
  properties[index] = { ...current, ...draft, id, agentId: current.agentId, createdAt: current.createdAt };
  saveCollection('properties', properties);
  return { ok: true, property: properties[index] };
}

/** Quick status change: 'available' | 'negotiation' | 'sold'. */
export function changePropertyStatus(id, status) {
  if (!(status in PROPERTY_STATUS_LABELS)) return { ok: false, error: 'Estado no válido' };
  const properties = getCollection('properties');
  const property = properties.find((item) => item.id === id);
  if (!property) return { ok: false, error: 'La propiedad ya no existe' };
  property.status = status;
  saveCollection('properties', properties);
  return { ok: true, property };
}

/** Number of leads that would be removed together with a property. */
export function countLinkedLeads(propertyId) {
  return getCollection('leads').filter((lead) => lead.propertyId === propertyId).length;
}

/**
 * Deletes a property and its linked leads (a lead without a property
 * cannot be worked). Returns { ok, property, removedLeads }.
 */
export function deleteProperty(id) {
  const properties = getCollection('properties');
  const property = properties.find((item) => item.id === id);
  if (!property) return { ok: false, error: 'La propiedad ya no existe' };
  const leads = getCollection('leads');
  const remainingLeads = leads.filter((lead) => lead.propertyId !== id);
  if (remainingLeads.length !== leads.length) saveCollection('leads', remainingLeads);
  saveCollection('properties', properties.filter((item) => item.id !== id));
  return { ok: true, property, removedLeads: leads.length - remainingLeads.length };
}
