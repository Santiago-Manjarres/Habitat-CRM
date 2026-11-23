/**
 * Keeps the latest copy of the data the Java server prepared (GET /api/estado)
 * so the screen can draw itself, and forwards every change to the server.
 * No business rule lives here: sorting, searching, validating, the board and
 * the summary figures are all computed in Java.
 */
import { LEAD_STAGE_LABELS, LEAD_SOURCE_LABELS } from '../config.js';
import { request } from './api.js';
import { notifyChange } from './syncService.js';

let current = null;

export const STAGE_OPTIONS = Object.entries(LEAD_STAGE_LABELS).map(([value, label]) => ({ value, label }));
export const SOURCE_OPTIONS = Object.entries(LEAD_SOURCE_LABELS).map(([value, label]) => ({ value, label }));

/** Downloads everything the screen needs. Returns false when it could not. */
export async function loadState() {
  const data = await request('GET', '/estado');
  if (!data.ok) return false;
  current = data;
  return true;
}

export function clearState() {
  current = null;
}

export const getVersion = () => current?.version ?? 0;
export const getProperties = () => current?.properties ?? [];
export const getLeads = () => current?.leads ?? [];
export const getAdvisors = () => current?.advisors ?? [];
export const getSummary = () => current.summary;
export const getBoard = () => current?.board ?? [];
export const getPropertyById = (id) => getProperties().find((property) => property.id === id) ?? null;
export const getLeadById = (id) => getLeads().find((lead) => lead.id === id) ?? null;

/** Property search runs in Java (GET /api/propiedades?q=...). */
export async function searchProperties(query) {
  const data = await request('GET', `/propiedades?q=${encodeURIComponent(query)}`);
  return data.ok ? data.properties : [];
}

/** Sends a change; when the server accepts it, reloads the data and redraws every tab. */
async function change(method, path, body) {
  const result = await request(method, path, body);
  if (result.ok) {
    await loadState();
    notifyChange('all');
  }
  return result;
}

export const createProperty = (values) => change('POST', '/propiedades', values);
export const updateProperty = (id, values) => change('PUT', `/propiedades/${encodeURIComponent(id)}`, values);
export const changePropertyStatus = (id, status) => change('PATCH', `/propiedades/${encodeURIComponent(id)}/estado`, { status });
export const deleteProperty = (id) => change('DELETE', `/propiedades/${encodeURIComponent(id)}`);

export const createLead = (values) => change('POST', '/clientes', values);
export const updateLead = (id, values) => change('PUT', `/clientes/${encodeURIComponent(id)}`, values);
export const updateLeadStage = (id, stage) => change('PATCH', `/clientes/${encodeURIComponent(id)}/etapa`, { stage });
export const deleteLead = (id) => change('DELETE', `/clientes/${encodeURIComponent(id)}`);

/** Live form checks while the advisor types: the server answers { errors }. */
export async function validateProperty(values) {
  return (await request('POST', '/validar/propiedad', values)).errors ?? {};
}

export async function validateLead(values) {
  return (await request('POST', '/validar/cliente', values)).errors ?? {};
}
