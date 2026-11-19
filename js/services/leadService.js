/**
 * Lead (prospect) management: "I'm interested" flow, funnel stages,
 * commercial notes and dashboard KPIs.
 */
import { LEAD_STAGE_LABELS, CLOSED_LEAD_STAGES, ROLES } from '../config.js';
import { isValidLeadStage, validateLeadNotes } from '../utils/validators.js';
import { getCollection, saveCollection, createId } from './storageService.js';
import { getPropertyById } from './propertyService.js';

/** Funnel columns (Spec §5.3.3): both closed stages share the "Cerrado" column. */
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

function toPublicUser(user) {
  if (!user) return null;
  const { id, name, email, phone, avatar, role } = user;
  return { id, name, email, phone, avatar, role };
}

function toPropertySummary(property) {
  if (!property) return null;
  const { id, title, location, price, currency, operation, status, type, imageUrl } = property;
  return { id, title, location, price, currency, operation, status, type, imageUrl };
}

/** Leads joined with their client and property summaries, most recent first. */
export function getLeadsWithDetails(leads = getLeads()) {
  const users = new Map(getCollection('users').map((user) => [user.id, user]));
  const properties = new Map(getCollection('properties').map((property) => [property.id, property]));
  return leads.map((lead) => ({
    ...lead,
    client: toPublicUser(users.get(lead.clientId)),
    agent: toPublicUser(users.get(lead.agentId)),
    property: toPropertySummary(properties.get(lead.propertyId)),
  }));
}

/** A client's own inquiries ("Mis consultas"). */
export function getLeadsByClient(clientId) {
  return getLeadsWithDetails(getLeads().filter((lead) => lead.clientId === clientId));
}

/** Groups leads into the funnel columns, keeping each column's order. */
export function groupLeadsByStage(leads) {
  return FUNNEL_COLUMNS.map((column) => ({
    ...column,
    leads: leads.filter((lead) => column.stages.includes(lead.stage)),
  }));
}

export function findActiveLead(clientId, propertyId) {
  return getCollection('leads').find(
    (lead) => lead.clientId === clientId && lead.propertyId === propertyId && isActiveLead(lead),
  ) ?? null;
}

/* ---------- Mutations ---------- */

/**
 * "Consultar / Me interesa": creates a lead in stage 'new' unless the client
 * already has an active one for that property. Returns { ok, lead, created }.
 */
export function expressInterest(client, propertyId) {
  if (client?.role !== ROLES.client) return { ok: false, error: 'Solo los clientes pueden enviar consultas' };
  const property = getPropertyById(propertyId);
  if (!property) return { ok: false, error: 'Esta propiedad ya no está publicada' };
  if (property.status === 'sold') return { ok: false, error: 'Esta propiedad ya fue vendida o arrendada' };
  const existing = findActiveLead(client.id, propertyId);
  if (existing) return { ok: true, lead: existing, created: false };
  const now = new Date().toISOString();
  const lead = {
    id: createId('lead'),
    clientId: client.id,
    agentId: property.agentId,
    propertyId,
    stage: 'new',
    notes: '',
    createdAt: now,
    updatedAt: now,
  };
  saveCollection('leads', [...getCollection('leads'), lead]);
  return { ok: true, lead, created: true };
}

function updateLead(id, changes) {
  const leads = getCollection('leads');
  const lead = leads.find((item) => item.id === id);
  if (!lead) return { ok: false, error: 'El lead ya no existe. Puede que lo hayan eliminado en otra pestaña' };
  Object.assign(lead, changes, { updatedAt: new Date().toISOString() });
  saveCollection('leads', leads);
  return { ok: true, lead };
}

export function updateLeadStage(id, stage) {
  if (!isValidLeadStage(stage)) return { ok: false, error: 'Etapa no válida' };
  return updateLead(id, { stage });
}

export function updateLeadNotes(id, notes) {
  const { valid, errors } = validateLeadNotes(notes);
  if (!valid) return { ok: false, error: errors.notes };
  return updateLead(id, { notes: String(notes ?? '').trim() });
}

/* ---------- KPIs ---------- */

/**
 * Dashboard metrics computed from current data:
 * total properties, available properties, value of available inventory
 * (properties for sale; rents are monthly fees and are counted apart),
 * active leads and new leads.
 */
export function getDashboardKpis() {
  const properties = getCollection('properties');
  const leads = getCollection('leads');
  const available = properties.filter((property) => property.status === 'available');
  const availableForSale = available.filter((property) => property.operation === 'sale');
  return {
    totalProperties: properties.length,
    availableProperties: available.length,
    availableInventoryValue: availableForSale.reduce((sum, property) => sum + property.price, 0),
    availableForSaleCount: availableForSale.length,
    availableForRentCount: available.length - availableForSale.length,
    activeLeads: leads.filter(isActiveLead).length,
    newLeads: leads.filter((lead) => lead.stage === 'new').length,
  };
}
