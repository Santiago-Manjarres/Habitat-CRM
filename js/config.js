/**
 * Habitat CRM: settings and Spanish labels used by the interface.
 * The data and every business rule live in the Java server (servidor-java/);
 * this file only holds what the screen needs to draw itself.
 */

export const APP_NAME = 'Habitat CRM';
export const LOCALE = 'es-CO';
export const DEFAULT_CURRENCY = 'COP';

export const API_URL = './api';
export const SESSION_KEY = 'inmocrm_active_session';
export const SYNC_CHANNEL_NAME = 'inmocrm_sync_channel';
export const SYNC_POLL_MS = 2000; // how often the browser asks the server for changes made elsewhere

export const PLACEHOLDER_IMAGE = './assets/img/property-placeholder.svg';

export const SEARCH_DEBOUNCE_MS = 200;
export const TOAST_DURATION_MS = 4000;
export const TOAST_ERROR_DURATION_MS = 6000;
export const TOAST_MAX_VISIBLE = 3;

/** Field lengths, used only for the maxlength attribute (the server checks them again). */
export const LIMITS = Object.freeze({ titleMax: 120, locationMax: 120, descriptionMax: 2000, notesMax: 1000, nameMax: 80 });

export const ROLES = Object.freeze({ admin: 'admin' });

export const ROLE_LABELS = Object.freeze({ admin: 'Asesor' });

export const PROPERTY_TYPES = Object.freeze(['Apartamento', 'Casa', 'Local', 'Oficina', 'Lote']);

export const OPERATION_LABELS = Object.freeze({
  sale: 'Venta',
  rent: 'Arriendo',
});

export const PROPERTY_STATUS_LABELS = Object.freeze({
  available: 'Disponible',
  negotiation: 'En negociación',
  sold: 'Vendida',
});

export const LEAD_STAGE_LABELS = Object.freeze({
  new: 'Nuevo',
  contacted: 'Contactado',
  visit: 'Visita',
  negotiation: 'Negociación',
  closed_won: 'Negocio cerrado',
  closed_lost: 'Descartado',
});

export const LEAD_SOURCE_LABELS = Object.freeze({
  web: 'Portal web',
  phone: 'Llamada',
  whatsapp: 'WhatsApp',
  walk_in: 'Visita a la oficina',
  referral: 'Referido',
});

/** Badge tone per state (Spec §9.5, "Badges de estado"). */
export const STATUS_TONES = Object.freeze({
  property: { available: 'success', negotiation: 'warning', sold: 'danger' },
  lead: {
    new: 'info',
    contacted: 'info',
    visit: 'warning',
    negotiation: 'warning',
    closed_won: 'success',
    closed_lost: 'neutral',
  },
});
