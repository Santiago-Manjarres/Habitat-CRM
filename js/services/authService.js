/**
 * Authentication: login against the stored users, per-tab session in
 * sessionStorage ('inmocrm_active_session'), logout and client self-registration.
 */
import { SESSION_KEY, ROLES } from '../config.js';
import { validateLoginForm, validateRegistrationForm } from '../utils/validators.js';
import { getInitials } from '../utils/formatters.js';
import { getCollection, saveCollection, createId } from './storageService.js';

export const LOGIN_ERROR_MESSAGE = 'Usuario o contraseña incorrectos';

let memorySession = null; // fallback when sessionStorage is blocked

function toSessionUser(user) {
  const { password, ...safeUser } = user;
  return safeUser;
}

function writeSession(sessionUser) {
  memorySession = sessionUser;
  try {
    sessionStorage.setItem(SESSION_KEY, JSON.stringify(sessionUser));
  } catch {
    /* sessionStorage blocked: keep the in-memory session for this page */
  }
}

function readSession() {
  try {
    const raw = sessionStorage.getItem(SESSION_KEY);
    return raw ? JSON.parse(raw) : null;
  } catch {
    return memorySession;
  }
}

function normalizeUsername(username) {
  return String(username ?? '').trim().toLowerCase();
}

function findUserByUsername(username) {
  const wanted = normalizeUsername(username);
  return getCollection('users').find((user) => normalizeUsername(user.username) === wanted) ?? null;
}

/**
 * Username is case-insensitive, password is case-sensitive.
 * Returns { ok: true, user } or { ok: false, error, fieldErrors? }.
 */
export function login(username, password) {
  const check = validateLoginForm({ username, password });
  if (!check.valid) return { ok: false, error: null, fieldErrors: check.errors };
  const user = findUserByUsername(username);
  if (!user || user.password !== password) return { ok: false, error: LOGIN_ERROR_MESSAGE };
  const sessionUser = toSessionUser(user);
  writeSession(sessionUser);
  return { ok: true, user: sessionUser };
}

export function logout() {
  memorySession = null;
  try {
    sessionStorage.removeItem(SESSION_KEY);
  } catch {
    /* nothing else to clear */
  }
}

/** Returns the active session user (without password) or null. */
export function getActiveSession() {
  const session = readSession();
  if (!session || typeof session.id !== 'string' || !Object.values(ROLES).includes(session.role)) return null;
  return session;
}

/**
 * Checks the session user still exists (it may have been removed by a
 * restore/import in another tab). Refreshes the stored profile if it changed.
 */
export function revalidateSession() {
  const session = getActiveSession();
  if (!session) return null;
  const user = getCollection('users').find((item) => item.id === session.id && item.role === session.role);
  if (!user) {
    logout();
    return null;
  }
  const fresh = toSessionUser(user);
  writeSession(fresh);
  return fresh;
}

export function isStaff(user) {
  return user?.role === ROLES.admin || user?.role === ROLES.agent;
}

export function getUserById(id) {
  const user = getCollection('users').find((item) => item.id === id);
  return user ? toSessionUser(user) : null;
}

function findDuplicateErrors(data) {
  const users = getCollection('users');
  const errors = {};
  if (findUserByUsername(data.username)) errors.username = 'Ese usuario ya existe. Prueba con otro';
  const email = String(data.email).trim().toLowerCase();
  if (users.some((user) => String(user.email ?? '').toLowerCase() === email)) {
    errors.email = 'Ya hay una cuenta con ese correo';
  }
  return errors;
}

/** Creates a client account and signs it in. Returns { ok, user } or { ok: false, fieldErrors }. */
export function registerClient(data) {
  const check = validateRegistrationForm(data);
  const fieldErrors = { ...check.errors, ...(check.valid ? findDuplicateErrors(data) : {}) };
  if (Object.keys(fieldErrors).length > 0) return { ok: false, fieldErrors };
  const name = data.name.trim();
  const user = {
    id: createId('usr'),
    username: data.username.trim(),
    password: data.password,
    name,
    email: data.email.trim(),
    phone: String(data.phone ?? '').trim(),
    role: ROLES.client,
    avatar: getInitials(name),
  };
  saveCollection('users', [...getCollection('users'), user]);
  const sessionUser = toSessionUser(user);
  writeSession(sessionUser);
  return { ok: true, user: sessionUser };
}
