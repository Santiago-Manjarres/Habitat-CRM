/**
 * Lead management. A lead is a contact an advisor registers (a call, a WhatsApp
 * message, a walk-in...) interested in one property, moving through the funnel.
 * Also computes the dashboard figures.
 */
import { LEAD_STAGE_LABELS, LEAD_SOURCE_LABELS, CLOSED_LEAD_STAGES } from '../config.js';
import { isValidLeadStage, validateLeadData, validateLeadNotes } from '../utils/validators.js';
import { getCollection, saveCollection, createId } from './storageService.js';

/** Funnel columns: both closed stages share the "Cerrado" column. */
export const FUNNEL_COLUMNS = Object.freeze([
  { key: 'new', label: 'Nuevo', stages: ['new'] },
  { key: 'contacted', label: 'Contactado', stages: ['contacted'] },
  { key: 'visit', label: 'Visita', stages: ['visit'] },
  { key: 'negotiation', label: 'Negociación', stages: ['negotiation'] },
  { key: 'closed', label: 'Cerrado', stages: ['closed_won', 'closed_lost'] },
]);

export const STAGE_OPTIONS = Object.freeze(
  Object.entries(LEAD_STAGE_LABELS).map(([value, label]) => ({ value, label })),
);

export const SOURCE_OPTIONS = Object.freeze(
  Object.entries(LEAD_SOURCE_LABELS).map(([value, label]) => ({ value, label })),
);

const TEXT_FIELDS = ['contactName', 'contactPhone', 'contactEmail', 'source', 'propertyId', 'agentId', 'stage', 'notes'];

export function isActiveLead(lead) {
  return !CLOSED_LEAD_STAGES.includes(lead.stage);
}

function byLastUpdate(a, b) {
  return String(b.updatedAt ?? b.createdAt).localeCompare(String(a.updatedAt ?? a.createdAt));
}

export function getLeads() {
  return getCollection('leads').sort(byLastUpdate);
}

export function getLeadById(id) {
  return getCollection('leads').find((lead) => lead.id === id) ?? null;
}

/* ---------- Joined views (never expose passwords) ---------- */

function toAdvisor(user) {
  if (!user) return null;
  const { id, name, avatar } = user;
  return { id, name, avatar };
}

function toPropertySummary(property) {
  if (!property) return null;
  const { id, title, location, price, currency, operation, status, type, imageUrl } = property;
  return { id, title, location, price, currency, operation, status, type, imageUrl };
}

/** Leads joined with their property summary and advisor, most recent first. */
export function getLeadsWithDetails(leads = getLeads()) {
  const users = new Map(getCollection('users').map((user) => [user.id, user]));
  const properties = new Map(getCollection('properties').map((property) => [property.id, property]));
  return leads.map((lead) => ({
    ...lead,
    agent: toAdvisor(users.get(lead.agentId)),
    property: toPropertySummary(properties.get(lead.propertyId)),
  }));
}

export function getRecentLeads(count = 5) {
  return getLeadsWithDetails(getLeads().slice(0, count));
}

/** Groups leads into the funnel columns, keeping each column's order. */
export function groupLeadsByStage(leads) {
  return FUNNEL_COLUMNS.map((column) => ({
    ...column,
    leads: leads.filter((lead) => column.stages.includes(lead.stage)),
  }));
}

/* ---------- Mutations ---------- */

/** Trims raw form values into a lead draft. */
export function normalizeLeadInput(raw) {
  const draft = {};
  TEXT_FIELDS.forEach((field) => { draft[field] = String(raw[field] ?? '').trim(); });
  draft.stage = draft.stage || 'new';
  return draft;
}

function checkLead(draft) {
  const { valid, errors } = validateLeadData(draft);
  if (!valid) return errors;
  const propertyExists = getCollection('properties').some((property) => property.id === draft.propertyId);
  const agentExists = getCollection('users').some((user) => user.id === draft.agentId);
  if (!propertyExists) return { propertyId: 'Esa propiedad ya no existe. Elige otra' };
  if (!agentExists) return { agentId: 'Ese asesor ya no existe. Elige otro' };
  return null;
}

/** Validates and registers a new lead. Returns { ok, lead } or { ok: false, errors }. */
export function createLead(rawInput) {
  const draft = normalizeLeadInput(rawInput);
  const errors = checkLead(draft);
  if (errors) return { ok: false, errors };
  const now = new Date().toISOString();
  const lead = { id: createId('lead'), ...draft, createdAt: now, updatedAt: now };
  saveCollection('leads', [...getCollection('leads'), lead]);
  return { ok: true, lead };
}

/** Validates and saves every field of an existing lead. */
export function updateLead(id, rawInput) {
  const leads = getCollection('leads');
  const index = leads.findIndex((lead) => lead.id === id);
  if (index === -1) return { ok: false, errors: { _form: 'Este cliente ya no existe. Puede que otro asesor lo haya eliminado' } };
  const draft = normalizeLeadInput(rawInput);
  const errors = checkLead(draft);
  if (errors) return { ok: false, errors };
  leads[index] = { ...leads[index], ...draft, updatedAt: new Date().toISOString() };
  saveCollection('leads', leads);
  return { ok: true, lead: leads[index] };
}

function patchLead(id, changes) {
  const leads = getCollection('leads');
  const lead = leads.find((item) => item.id === id);
  if (!lead) return { ok: false, error: 'Este cliente ya no existe. Puede que otro asesor lo haya eliminado' };
  Object.assign(lead, changes, { updatedAt: new Date().toISOString() });
  saveCollection('leads', leads);
  return { ok: true, lead };
}

export function updateLeadStage(id, stage) {
  if (!isValidLeadStage(stage)) return { ok: false, error: 'Etapa no válida' };
  return patchLead(id, { stage });
}

export function updateLeadNotes(id, notes) {
  const { valid, errors } = validateLeadNotes(notes);
  if (!valid) return { ok: false, error: errors.notes };
  return patchLead(id, { notes: String(notes ?? '').trim() });
}

export function deleteLead(id) {
  const leads = getCollection('leads');
  const lead = leads.find((item) => item.id === id);
  if (!lead) return { ok: false, error: 'Este cliente ya no existe' };
  saveCollection('leads', leads.filter((item) => item.id !== id));
  return { ok: true, lead };
}

/* ---------- Dashboard figures ---------- */

/**
 * Figures computed from current data: available inventory (count and value of
 * properties for sale; rents are monthly fees and are counted apart), active and
 * new leads, closed deals and the share of closed leads that were won.
 */
export function getDashboardKpis() {
  const properties = getCollection('properties');
  const leads = getCollection('leads');
  const available = properties.filter((property) => property.status === 'available');
  const availableForSale = available.filter((property) => property.operation === 'sale');
  const won = leads.filter((lead) => lead.stage === 'closed_won').length;
  const closed = leads.filter((lead) => CLOSED_LEAD_STAGES.includes(lead.stage)).length;
  return {
    totalProperties: properties.length,
    availableProperties: available.length,
    availableInventoryValue: availableForSale.reduce((sum, property) => sum + property.price, 0),
    availableForSaleCount: availableForSale.length,
    availableForRentCount: available.length - availableForSale.length,
    activeLeads: leads.filter(isActiveLead).length,
    newLeads: leads.filter((lead) => lead.stage === 'new').length,
    wonDeals: won,
    closedDeals: closed,
    winRate: closed ? Math.round((won / closed) * 100) : null,
  };
}

/** Lead count per funnel column, for the dashboard chart. */
export function getFunnelCounts() {
  const leads = getCollection('leads');
  return FUNNEL_COLUMNS.map((column) => ({
    key: column.key,
    label: column.label,
    count: leads.filter((lead) => column.stages.includes(lead.stage)).length,
  }));
}
