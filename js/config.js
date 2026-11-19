/**
 * Habitat CRM — Global configuration.
 * Storage keys, sync channel, enums with Spanish UI labels and the embedded
 * seed data used as a fallback when /data/*.json cannot be fetched (file://).
 * SEED_DATA must stay identical to the files in /data/.
 */

export const APP_NAME = 'Habitat CRM';
export const LOCALE = 'es-CO';
export const DEFAULT_CURRENCY = 'COP';

export const STORAGE_KEYS = Object.freeze({
  users: 'inmocrm_users',
  properties: 'inmocrm_properties',
  leads: 'inmocrm_leads',
});

export const SESSION_KEY = 'inmocrm_active_session';
export const SYNC_CHANNEL_NAME = 'inmocrm_sync_channel';

export const ENTITIES = Object.freeze(['users', 'properties', 'leads']);

export const DATA_FILES = Object.freeze({
  users: './data/users.json',
  properties: './data/properties.json',
  leads: './data/leads.json',
});

export const PLACEHOLDER_IMAGE = './assets/img/property-placeholder.svg';
export const LOGO_IMAGE = './assets/img/logo.png';
export const ICON_SPRITE = './assets/icons/sprite.svg';

export const SEARCH_DEBOUNCE_MS = 200;
export const TOAST_DURATION_MS = 4000;
export const TOAST_ERROR_DURATION_MS = 6000;
export const TOAST_MAX_VISIBLE = 3;

export const ROLES = Object.freeze({ admin: 'admin', agent: 'agent', client: 'client' });

export const ROLE_LABELS = Object.freeze({
  admin: 'Asesor',
  agent: 'Asesor',
  client: 'Cliente',
});

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
  closed_won: 'Cerrado ganado',
  closed_lost: 'Descartado',
});

export const CLOSED_LEAD_STAGES = Object.freeze(['closed_won', 'closed_lost']);

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

const UNSPLASH = 'https://images.unsplash.com/';
const IMG_PARAMS = '?auto=format&fit=crop&w=800&q=80';

/** Embedded seed data — fallback for file:// (fetch blocked by CORS). */
export const SEED_DATA = Object.freeze({
  users: [
    {
      id: 'usr_admin_01',
      username: 'admin',
      password: 'admin',
      name: 'Laura Mendoza (Asesora)',
      email: 'admin@inmocrm.com',
      phone: '+57 300 111 2233',
      role: 'admin',
      avatar: 'LM',
    },
    {
      id: 'usr_client_01',
      username: 'client',
      password: 'client',
      name: 'Carlos Pérez',
      email: 'carlos.perez@correo.com',
      phone: '+57 310 444 5566',
      role: 'client',
      avatar: 'CP',
    },
    {
      id: 'usr_client_02',
      username: 'ana.gomez',
      password: '1234',
      name: 'Ana Gómez',
      email: 'ana.gomez@correo.com',
      phone: '+57 320 777 8899',
      role: 'client',
      avatar: 'AG',
    },
  ],
  properties: [
    {
      id: 'prop_001',
      title: 'Apartamento Moderno con Vista al Mar',
      type: 'Apartamento',
      operation: 'sale',
      price: 450000000,
      currency: 'COP',
      location: 'Bocagrande, Cartagena',
      areaM2: 115,
      bedrooms: 3,
      bathrooms: 2,
      parking: 1,
      status: 'available',
      agentId: 'usr_admin_01',
      description: 'Amplio apartamento iluminado con balcón panorámico, cocina integral y zonas sociales completas.',
      imageUrl: `${UNSPLASH}photo-1522708323590-d24dbb6b0267${IMG_PARAMS}`,
      createdAt: '2026-10-15T10:00:00.000Z',
    },
    {
      id: 'prop_002',
      title: 'Casa Campestre con Jardín y Chimenea',
      type: 'Casa',
      operation: 'sale',
      price: 890000000,
      currency: 'COP',
      location: 'Llanogrande, Rionegro',
      areaM2: 280,
      bedrooms: 4,
      bathrooms: 4,
      parking: 2,
      status: 'available',
      agentId: 'usr_admin_01',
      description: 'Casa de dos niveles rodeada de zonas verdes, con chimenea, estudio, cuarto de servicio y amplio jardín privado.',
      imageUrl: `${UNSPLASH}photo-1568605114967-8130f3a36994${IMG_PARAMS}`,
      createdAt: '2026-10-18T15:30:00.000Z',
    },
    {
      id: 'prop_003',
      title: 'Apartaestudio Amoblado en Chapinero Alto',
      type: 'Apartamento',
      operation: 'rent',
      price: 2300000,
      currency: 'COP',
      location: 'Chapinero Alto, Bogotá',
      areaM2: 42,
      bedrooms: 1,
      bathrooms: 1,
      parking: 0,
      status: 'available',
      agentId: 'usr_admin_01',
      description: 'Apartaestudio completamente amoblado, a pasos de universidades y zonas gastronómicas. Incluye administración.',
      imageUrl: `${UNSPLASH}photo-1502672260266-1c1ef2d93688${IMG_PARAMS}`,
      createdAt: '2026-10-22T09:00:00.000Z',
    },
    {
      id: 'prop_004',
      title: 'Local Comercial sobre Avenida Principal',
      type: 'Local',
      operation: 'rent',
      price: 6500000,
      currency: 'COP',
      location: 'El Poblado, Medellín',
      areaM2: 90,
      bedrooms: 0,
      bathrooms: 1,
      parking: 0,
      status: 'negotiation',
      agentId: 'usr_admin_01',
      description: 'Local esquinero con vitrina doble, alto flujo peatonal y vehicular. Ideal para restaurante o tienda.',
      imageUrl: `${UNSPLASH}photo-1441986300917-64674bd600d8${IMG_PARAMS}`,
      createdAt: '2026-10-25T11:45:00.000Z',
    },
    {
      id: 'prop_005',
      title: 'Casa Familiar en Conjunto Cerrado',
      type: 'Casa',
      operation: 'sale',
      price: 620000000,
      currency: 'COP',
      location: 'Ciudad Jardín, Cali',
      areaM2: 190,
      bedrooms: 4,
      bathrooms: 3,
      parking: 2,
      status: 'negotiation',
      agentId: 'usr_admin_01',
      description: 'Casa en conjunto con piscina, parque infantil y vigilancia 24 horas. Patio interior y terraza cubierta.',
      imageUrl: `${UNSPLASH}photo-1570129477492-45c003edd2be${IMG_PARAMS}`,
      createdAt: '2026-10-28T14:00:00.000Z',
    },
    {
      id: 'prop_006',
      title: 'Oficina Ejecutiva en Torre Empresarial',
      type: 'Oficina',
      operation: 'rent',
      price: 4800000,
      currency: 'COP',
      location: 'Santa Bárbara, Bogotá',
      areaM2: 75,
      bedrooms: 0,
      bathrooms: 2,
      parking: 2,
      status: 'available',
      agentId: 'usr_admin_01',
      description: 'Oficina con divisiones en vidrio, sala de juntas, recepción y acceso a auditorio compartido del edificio.',
      imageUrl: `${UNSPLASH}photo-1497366216548-37526070297c${IMG_PARAMS}`,
      createdAt: '2026-11-02T08:30:00.000Z',
    },
    {
      id: 'prop_007',
      title: 'Penthouse Dúplex con Terraza Privada',
      type: 'Apartamento',
      operation: 'sale',
      price: 1350000000,
      currency: 'COP',
      location: 'Rosales, Bogotá',
      areaM2: 210,
      bedrooms: 3,
      bathrooms: 4,
      parking: 3,
      status: 'sold',
      agentId: 'usr_admin_01',
      description: 'Penthouse de dos niveles con terraza de 60 m², jacuzzi, vista a los cerros orientales y acabados de lujo.',
      imageUrl: `${UNSPLASH}photo-1600596542815-ffad4c1539a9${IMG_PARAMS}`,
      createdAt: '2026-11-05T16:10:00.000Z',
    },
    {
      id: 'prop_008',
      title: 'Apartamento para Estrenar cerca al Metro',
      type: 'Apartamento',
      operation: 'rent',
      price: 1900000,
      currency: 'COP',
      location: 'Envigado, Antioquia',
      areaM2: 68,
      bedrooms: 2,
      bathrooms: 2,
      parking: 1,
      status: 'available',
      agentId: 'usr_admin_01',
      description: 'Apartamento nuevo con balcón, cocina abierta y zonas comunes con gimnasio. A cinco minutos de la estación.',
      imageUrl: `${UNSPLASH}photo-1560448204-e02f11c3d0e2${IMG_PARAMS}`,
      createdAt: '2026-11-08T10:20:00.000Z',
    },
    {
      id: 'prop_009',
      title: 'Local en el Centro Histórico',
      type: 'Local',
      operation: 'sale',
      price: 380000000,
      currency: 'COP',
      location: 'Centro Histórico, Cartagena',
      areaM2: 55,
      bedrooms: 0,
      bathrooms: 1,
      parking: 0,
      status: 'available',
      agentId: 'usr_admin_01',
      description: 'Local en casona colonial restaurada, techos altos y fachada protegida. Excelente para café o galería.',
      imageUrl: `${UNSPLASH}photo-1555396273-367ea4eb4db5${IMG_PARAMS}`,
      createdAt: '2026-11-12T13:00:00.000Z',
    },
  ],
  leads: [
    {
      id: 'lead_001',
      clientId: 'usr_client_01',
      agentId: 'usr_admin_01',
      propertyId: 'prop_001',
      stage: 'contacted',
      notes: 'Cliente interesado en financiar el 70% con crédito hipotecario.',
      createdAt: '2026-11-10T14:30:00.000Z',
      updatedAt: '2026-11-12T09:15:00.000Z',
    },
    {
      id: 'lead_002',
      clientId: 'usr_client_02',
      agentId: 'usr_admin_01',
      propertyId: 'prop_005',
      stage: 'visit',
      notes: 'Visita agendada para el sábado a las 10:00 a. m.',
      createdAt: '2026-11-11T10:00:00.000Z',
      updatedAt: '2026-11-14T17:40:00.000Z',
    },
    {
      id: 'lead_003',
      clientId: 'usr_client_01',
      agentId: 'usr_admin_01',
      propertyId: 'prop_004',
      stage: 'new',
      notes: '',
      createdAt: '2026-11-17T08:05:00.000Z',
      updatedAt: '2026-11-17T08:05:00.000Z',
    },
    {
      id: 'lead_004',
      clientId: 'usr_client_02',
      agentId: 'usr_admin_01',
      propertyId: 'prop_002',
      stage: 'negotiation',
      notes: 'Ofertó $850 millones; pendiente respuesta del propietario.',
      createdAt: '2026-11-06T09:30:00.000Z',
      updatedAt: '2026-11-16T12:00:00.000Z',
    },
    {
      id: 'lead_005',
      clientId: 'usr_client_02',
      agentId: 'usr_admin_01',
      propertyId: 'prop_007',
      stage: 'closed_won',
      notes: 'Escrituración firmada en notaría.',
      createdAt: '2026-10-20T15:00:00.000Z',
      updatedAt: '2026-11-13T11:30:00.000Z',
    },
    {
      id: 'lead_006',
      clientId: 'usr_client_01',
      agentId: 'usr_admin_01',
      propertyId: 'prop_003',
      stage: 'closed_lost',
      notes: 'Prefirió una opción más cerca de su trabajo.',
      createdAt: '2026-11-01T16:20:00.000Z',
      updatedAt: '2026-11-09T10:10:00.000Z',
    },
  ],
});
