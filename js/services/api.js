/**
 * The one place that talks to the Java server (servidor-java/).
 * Sends JSON, receives JSON, and attaches the session code in the "X-Sesion" header.
 */
import { API_URL, SESSION_KEY } from '../config.js';

let onSessionExpired = () => {};

/** app.js decides what happens when the server says the session is over. */
export function setSessionExpiredHandler(handler) {
  onSessionExpired = handler;
}

export function readToken() {
  try {
    return JSON.parse(sessionStorage.getItem(SESSION_KEY) ?? 'null')?.token ?? '';
  } catch {
    return '';
  }
}

/**
 * request('POST', '/clientes', { ... }) -> the server's JSON answer.
 * Never throws: a lost connection comes back as { ok: false, error }.
 */
export async function request(method, path, body) {
  try {
    const response = await fetch(`${API_URL}${path}`, {
      method,
      headers: { 'Content-Type': 'application/json', 'X-Sesion': readToken() },
      body: body === undefined ? undefined : JSON.stringify(body),
      cache: 'no-store',
    });
    const data = await response.json();
    if (response.status === 401 && data.sessionExpired) onSessionExpired();
    return data;
  } catch {
    return { ok: false, error: 'No se pudo conectar con el servidor. Revisa que Habitat CRM esté encendido' };
  }
}
