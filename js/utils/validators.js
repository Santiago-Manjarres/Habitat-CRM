/**
 * Form and data validation. Every validator returns
 * { valid: boolean, errors: { [field]: 'Mensaje en español' } }.
 * Pure functions — no DOM, no storage.
 */
import {
  PROPERTY_TYPES,
  OPERATION_LABELS,
  PROPERTY_STATUS_LABELS,
  LEAD_STAGE_LABELS,
  ROLES,
  ENTITIES,
} from '../config.js';

export const LIMITS = Object.freeze({
  titleMax: 120,
  locationMax: 120,
  descriptionMax: 2000,
  notesMax: 1000,
  nameMax: 80,
  usernameMin: 3,
  usernameMax: 30,
  passwordMin: 4,
  priceMax: 1e13,
  areaMax: 1e6,
  roomsMax: 99,
});

export function isRequired(value) {
  return String(value ?? '').trim().length > 0;
}

export function isPositiveNumber(value) {
  return typeof value === 'number' && Number.isFinite(value) && value > 0;
}

export function isNonNegativeInteger(value) {
  return Number.isInteger(value) && value >= 0;
}

/** Only absolute http(s) URLs are accepted. */
export function isValidUrl(value) {
  try {
    const url = new URL(String(value).trim());
    return (url.protocol === 'http:' || url.protocol === 'https:') && url.hostname.includes('.');
  } catch {
    return false;
  }
}

export function isValidEmail(value) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/.test(String(value ?? '').trim());
}

export function isValidPhone(value) {
  return /^\+?[\d\s-]{7,20}$/.test(String(value ?? '').trim());
}

function buildResult(errors) {
  return { valid: Object.keys(errors).length === 0, errors };
}

export function validateLoginForm({ username, password }) {
  const errors = {};
  if (!isRequired(username)) errors.username = 'Escribe tu usuario';
  if (!isRequired(password)) errors.password = 'Escribe tu contraseña';
  return buildResult(errors);
}

export function validateRegistrationForm(data) {
  const errors = {};
  const username = String(data.username ?? '').trim();
  if (!isRequired(data.name)) errors.name = 'Escribe tu nombre completo';
  else if (data.name.trim().length > LIMITS.nameMax) errors.name = `Usa máximo ${LIMITS.nameMax} caracteres`;
  if (!/^[a-zA-Z0-9._-]+$/.test(username)) errors.username = 'Usa solo letras, números, punto, guion o guion bajo';
  else if (username.length < LIMITS.usernameMin || username.length > LIMITS.usernameMax) {
    errors.username = `El usuario debe tener entre ${LIMITS.usernameMin} y ${LIMITS.usernameMax} caracteres`;
  }
  if (!isValidEmail(data.email)) errors.email = 'Escribe un correo válido, por ejemplo nombre@correo.com';
  if (isRequired(data.phone) && !isValidPhone(data.phone)) errors.phone = 'Escribe un teléfono válido, por ejemplo +57 300 123 4567';
  if (String(data.password ?? '').length < LIMITS.passwordMin) {
    errors.password = `La contraseña debe tener al menos ${LIMITS.passwordMin} caracteres`;
  }
  if (data.confirmPassword !== data.password) errors.confirmPassword = 'Las contraseñas no coinciden';
  return buildResult(errors);
}

function validatePropertyText(data, errors) {
  if (!isRequired(data.title)) errors.title = 'El título es obligatorio';
  else if (data.title.trim().length > LIMITS.titleMax) errors.title = `Usa máximo ${LIMITS.titleMax} caracteres`;
  if (!isRequired(data.location)) errors.location = 'La ubicación es obligatoria';
  else if (data.location.trim().length > LIMITS.locationMax) errors.location = `Usa máximo ${LIMITS.locationMax} caracteres`;
  if (String(data.description ?? '').length > LIMITS.descriptionMax) {
    errors.description = `Usa máximo ${LIMITS.descriptionMax} caracteres`;
  }
  if (!PROPERTY_TYPES.includes(data.type)) errors.type = 'Elige un tipo de inmueble';
  if (!(data.operation in OPERATION_LABELS)) errors.operation = 'Elige si es venta o arriendo';
  if (!(data.status in PROPERTY_STATUS_LABELS)) errors.status = 'Elige un estado válido';
  if (isRequired(data.imageUrl) && !isValidUrl(data.imageUrl)) {
    errors.imageUrl = 'La URL de la imagen no es válida. Debe empezar por http:// o https://';
  }
}

function validatePropertyNumbers(data, errors) {
  if (!isPositiveNumber(data.price)) errors.price = 'El precio debe ser mayor que 0';
  else if (data.price > LIMITS.priceMax) errors.price = 'El precio es demasiado alto';
  if (!isPositiveNumber(data.areaM2)) errors.areaM2 = 'El área debe ser mayor que 0';
  else if (data.areaM2 > LIMITS.areaMax) errors.areaM2 = 'El área es demasiado grande';
  const rooms = { bedrooms: 'habitaciones', bathrooms: 'baños', parking: 'parqueaderos' };
  for (const [field, label] of Object.entries(rooms)) {
    if (!isNonNegativeInteger(data[field])) errors[field] = `El número de ${label} debe ser 0 o más (sin decimales)`;
    else if (data[field] > LIMITS.roomsMax) errors[field] = `Máximo ${LIMITS.roomsMax} ${label}`;
  }
}

/** Expects numeric fields already parsed to numbers. */
export function validatePropertyData(data) {
  const errors = {};
  validatePropertyText(data, errors);
  validatePropertyNumbers(data, errors);
  return buildResult(errors);
}

export function validateLeadNotes(notes) {
  const errors = {};
  if (String(notes ?? '').length > LIMITS.notesMax) errors.notes = `Usa máximo ${LIMITS.notesMax} caracteres`;
  return buildResult(errors);
}

export function isValidLeadStage(stage) {
  return stage in LEAD_STAGE_LABELS;
}

/* ---------- Backup (import) structure validation ---------- */

const REQUIRED_FIELDS = {
  users: { id: 'string', username: 'string', password: 'string', name: 'string', role: 'string' },
  properties: {
    id: 'string', title: 'string', type: 'string', operation: 'string', price: 'number',
    location: 'string', areaM2: 'number', bedrooms: 'number', bathrooms: 'number',
    status: 'string', agentId: 'string',
  },
  leads: { id: 'string', clientId: 'string', propertyId: 'string', stage: 'string' },
};

function hasRequiredFields(item, schema) {
  if (!item || typeof item !== 'object' || Array.isArray(item)) return false;
  return Object.entries(schema).every(([key, type]) => typeof item[key] === type);
}

function findInvalidItem(entity, items) {
  const index = items.findIndex((item) => !hasRequiredFields(item, REQUIRED_FIELDS[entity]));
  return index === -1 ? null : index;
}

function validateEnums(data) {
  if (data.users.some((u) => !Object.values(ROLES).includes(u.role))) return 'Hay usuarios con un rol desconocido';
  if (!data.users.some((u) => u.role !== ROLES.client)) return 'El respaldo no contiene ningún asesor';
  if (data.properties.some((p) => !(p.status in PROPERTY_STATUS_LABELS))) return 'Hay propiedades con un estado desconocido';
  if (data.properties.some((p) => !(p.operation in OPERATION_LABELS))) return 'Hay propiedades con una operación desconocida';
  if (data.leads.some((l) => !isValidLeadStage(l.stage))) return 'Hay leads con una etapa desconocida';
  return null;
}

/** Validates an imported backup before it is applied. */
export function validateBackupStructure(data) {
  if (!data || typeof data !== 'object' || Array.isArray(data)) {
    return { valid: false, error: 'El archivo no tiene el formato de un respaldo de Habitat CRM' };
  }
  for (const entity of ENTITIES) {
    if (!Array.isArray(data[entity])) return { valid: false, error: `Falta la colección "${entity}" en el archivo` };
    const invalidIndex = findInvalidItem(entity, data[entity]);
    if (invalidIndex !== null) {
      return { valid: false, error: `El registro ${invalidIndex + 1} de "${entity}" está incompleto o tiene tipos incorrectos` };
    }
  }
  const enumError = validateEnums(data);
  return enumError ? { valid: false, error: enumError } : { valid: true, error: null };
}
