/**
 * Session of this tab. The Java server checks the username and password and
 * hands back the advisor and a session code; this tab keeps both in
 * sessionStorage ('inmocrm_active_session') so each tab can have its own advisor.
 */
import { SESSION_KEY } from '../config.js';
import { request } from './api.js';
import { getAdvisors } from './dataService.js';

function readSession() {
  try {
    return JSON.parse(sessionStorage.getItem(SESSION_KEY) ?? 'null');
  } catch {
    return null;
  }
}

function writeSession(session) {
  try {
    sessionStorage.setItem(SESSION_KEY, JSON.stringify(session));
  } catch {
    /* storage blocked: the session only lasts while the page is open */
  }
}

/** Returns { ok, user } or { ok: false, error, fieldErrors }. */
export async function login(username, password) {
  const result = await request('POST', '/sesion', { username, password });
  if (result.ok) writeSession({ user: result.user, token: result.token });
  return result;
}

export function logout() {
  if (readSession()) request('DELETE', '/sesion');
  try {
    sessionStorage.removeItem(SESSION_KEY);
  } catch {
    /* nothing else to clear */
  }
}

/** The advisor of this tab, or null. */
export function getActiveSession() {
  const session = readSession();
  return session?.user && session.token ? session.user : null;
}

/** Refreshes the stored profile with the latest advisor list sent by the server. */
export function revalidateSession() {
  const user = getActiveSession();
  if (!user) return null;
  const advisors = getAdvisors();
  if (!advisors.length) return user;
  const fresh = advisors.find((advisor) => advisor.id === user.id);
  if (!fresh) {
    logout();
    return null;
  }
  writeSession({ ...readSession(), user: fresh });
  return fresh;
}

export function isStaff(user) {
  return user?.role === 'admin';
}
