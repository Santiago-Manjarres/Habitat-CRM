/**
 * Advisor / admin panel: KPI dashboard, property CRUD (table + modal form),
 * lead funnel (kanban with stage changes and notes) and JSON data tools.
 */
import {
  PROPERTY_TYPES, OPERATION_LABELS, PROPERTY_STATUS_LABELS, LEAD_STAGE_LABELS,
  STATUS_TONES, SEARCH_DEBOUNCE_MS, ENTITIES,
} from '../config.js';
import {
  escapeHTML, iconMarkup, badgeMarkup, imageMarkup, formatCurrency, formatCompactCurrency,
  formatNumber, formatRelativeTime, parseNumber, pluralize,
} from '../utils/formatters.js';
import { validatePropertyData, LIMITS } from '../utils/validators.js';
import {
  getProperties, getPropertyById, filterProperties, createProperty, updateProperty,
  changePropertyStatus, deleteProperty, countLinkedLeads, normalizePropertyInput,
} from '../services/propertyService.js';
import {
  getLeadsWithDetails, groupLeadsByStage, updateLeadStage, updateLeadNotes, getLeadById,
  getDashboardKpis, STAGE_OPTIONS,
} from '../services/leadService.js';
import { exportBackup, importBackup, resetToSeed, getCollection } from '../services/storageService.js';
import { showToast, openModal, confirmDialog } from './toastUI.js';
import { showFieldErrors, enableLiveValidation, setButtonLoading } from './authUI.js';

const state = { user: null, propertyQuery: '', bound: false };

const roots = {
  summary: () => document.getElementById('admin-summary-root'),
  properties: () => document.getElementById('admin-properties-root'),
  leads: () => document.getElementById('admin-leads-root'),
  data: () => document.getElementById('admin-data-root'),
};

/* ==========================================================================
   Helpers
   ========================================================================== */

function emptyStateMarkup({ icon, title, text, action = '' }) {
  return `
    <div class="card empty-state">
      ${iconMarkup(icon, 'empty-state__icon')}
      <h2 class="empty-state__title">${escapeHTML(title)}</h2>
      <p class="empty-state__text">${escapeHTML(text)}</p>
      ${action}
    </div>`;
}

function optionsMarkup(entries, selected) {
  return entries.map(([value, label]) => `<option value="${escapeHTML(value)}"${value === selected ? ' selected' : ''}>${escapeHTML(label)}</option>`).join('');
}

/** Re-renders a container keeping focus on the element with the same data-focus-key. */
function renderKeepingFocus(container, html) {
  const key = container.contains(document.activeElement) ? document.activeElement.dataset.focusKey : null;
  container.innerHTML = html;
  if (key) container.querySelector(`[data-focus-key="${CSS.escape(key)}"]`)?.focus();
}

function debounce(fn, wait) {
  let handle = null;
  return (...args) => {
    clearTimeout(handle);
    handle = setTimeout(() => fn(...args), wait);
  };
}

/* ==========================================================================
   Summary (KPIs)
   ========================================================================== */

function kpiMarkup({ icon, label, value, hint }) {
  return `
    <article class="card kpi">
      <h2 class="kpi__label">${iconMarkup(icon, 'icon--sm')}${escapeHTML(label)}</h2>
      <p class="kpi__value">${escapeHTML(value)}</p>
      <p class="kpi__hint">${escapeHTML(hint)}</p>
    </article>`;
}

function renderSummary() {
  const k = getDashboardKpis();
  const cards = [
    { icon: 'building', label: 'Propiedades', value: formatNumber(k.totalProperties), hint: 'Total en el inventario' },
    { icon: 'check-circle', label: 'Disponibles', value: formatNumber(k.availableProperties), hint: `${k.availableForSaleCount} en venta · ${k.availableForRentCount} en arriendo` },
    { icon: 'sparkle', label: 'Valor del inventario disponible', value: formatCompactCurrency(k.availableInventoryValue), hint: `${formatCurrency(k.availableInventoryValue)} en venta` },
    { icon: 'funnel', label: 'Leads activos', value: formatNumber(k.activeLeads), hint: 'Sin contar los cerrados' },
    { icon: 'inbox', label: 'Leads nuevos', value: formatNumber(k.newLeads), hint: k.newLeads ? 'Esperan primer contacto' : 'Todo al día' },
  ];
  roots.summary().innerHTML = `<div class="kpi-grid">${cards.map(kpiMarkup).join('')}</div>`;
}

/* ==========================================================================
   Properties: table
   ========================================================================== */

function propertiesShellMarkup() {
  return `
    <div class="section-stack">
      <div class="toolbar">
        <div class="field search-field">
          <label class="field__label" for="admin-property-search">Buscar propiedad</label>
          <div class="field__affix field__affix--prefix">
            <span class="field__prefix">${iconMarkup('search', 'icon--sm')}</span>
            <input class="field__control" id="admin-property-search" type="search" placeholder="Título o ubicación" autocomplete="off">
          </div>
        </div>
        <button type="button" class="btn btn--primary" data-action="new-property">${iconMarkup('plus')}Nueva propiedad</button>
      </div>
      <p class="results-count" id="admin-property-count" aria-live="polite"></p>
      <div id="admin-property-table"></div>
    </div>`;
}

function propertyRowMarkup(p) {
  const id = escapeHTML(p.id);
  return `
    <tr>
      <td class="is-full">
        <span class="property-cell">
          ${imageMarkup(p.imageUrl, { className: 'property-cell__thumb', width: 48, height: 36 })}
          <span class="property-cell__text">
            <span class="property-cell__title">${escapeHTML(p.title)}</span>
            <span class="text-sm text-muted">${escapeHTML(p.location)}</span>
          </span>
        </span>
      </td>
      <td data-label="Tipo">${escapeHTML(p.type)}</td>
      <td data-label="Operación">${escapeHTML(OPERATION_LABELS[p.operation])}</td>
      <td data-label="Precio" class="is-numeric">${escapeHTML(formatCurrency(p.price, p.currency))}</td>
      <td data-label="Estado">
        <div class="field__select field__select--sm">
          <select class="field__control field__control--sm" data-action="status" data-id="${id}" data-focus-key="status-${id}" aria-label="Estado de ${escapeHTML(p.title)}">
            ${optionsMarkup(Object.entries(PROPERTY_STATUS_LABELS), p.status)}
          </select>
        </div>
      </td>
      <td class="is-full">
        <span class="data-table__actions">
          <button type="button" class="btn btn--ghost btn--sm" data-action="edit" data-id="${id}" data-focus-key="edit-${id}">${iconMarkup('edit', 'icon--sm')}Editar</button>
          <button type="button" class="btn btn--ghost btn--sm" data-action="delete" data-id="${id}" data-focus-key="delete-${id}">${iconMarkup('trash', 'icon--sm')}Eliminar</button>
        </span>
      </td>
    </tr>`;
}

function propertyTableMarkup(rows) {
  return `
    <div class="card table-card">
      <table class="data-table">
        <caption class="visually-hidden">Inventario de propiedades</caption>
        <thead>
          <tr><th scope="col">Propiedad</th><th scope="col">Tipo</th><th scope="col">Operación</th>
            <th scope="col" class="is-numeric">Precio</th><th scope="col">Estado</th><th scope="col"><span class="visually-hidden">Acciones</span></th></tr>
        </thead>
        <tbody>${rows.map(propertyRowMarkup).join('')}</tbody>
      </table>
    </div>`;
}

function renderPropertyTable() {
  const container = document.getElementById('admin-property-table');
  if (!container) return;
  const all = getProperties();
  const rows = filterProperties(all, { query: state.propertyQuery });
  document.getElementById('admin-property-count').textContent = pluralize(rows.length, 'propiedad', 'propiedades');
  if (all.length === 0) {
    container.innerHTML = emptyStateMarkup({ icon: 'building', title: 'Aún no hay propiedades', text: 'Crea la primera para publicarla en el catálogo de clientes.', action: `<button type="button" class="btn btn--primary" data-action="new-property">${iconMarkup('plus')}Crear propiedad</button>` });
    return;
  }
  if (rows.length === 0) {
    container.innerHTML = emptyStateMarkup({ icon: 'search', title: 'Sin resultados', text: 'Ninguna propiedad coincide con la búsqueda.', action: '<button type="button" class="btn btn--secondary" data-action="clear-search">Limpiar búsqueda</button>' });
    return;
  }
  renderKeepingFocus(container, propertyTableMarkup(rows));
}

/* ==========================================================================
   Properties: form (create / edit)
   ========================================================================== */

function inputField({ name, label, value = '', type = 'text', full = false, prefix = '', help = '', extra = '' }) {
  const id = `pf-${name}`;
  const describedBy = `${help ? `${id}-help ` : ''}${id}-error`;
  const input = `<input class="field__control" id="${id}" name="${name}" type="${type}" value="${escapeHTML(value)}" aria-describedby="${describedBy}" ${extra}>`;
  return `
    <div class="field${full ? ' is-full' : ''}">
      <label class="field__label" for="${id}">${label}</label>
      ${prefix ? `<div class="field__affix field__affix--prefix"><span class="field__prefix">${prefix}</span>${input}</div>` : input}
      ${help ? `<span class="field__help" id="${id}-help">${help}</span>` : ''}
      <span class="field__error" id="${id}-error"></span>
    </div>`;
}

function selectFormField(name, label, entries, selected) {
  const id = `pf-${name}`;
  return `
    <div class="field">
      <label class="field__label" for="${id}">${label}</label>
      <div class="field__select"><select class="field__control" id="${id}" name="${name}" aria-describedby="${id}-error">${optionsMarkup(entries, selected)}</select></div>
      <span class="field__error" id="${id}-error"></span>
    </div>`;
}

function propertyFormMarkup(p = {}) {
  const num = (value) => (Number.isFinite(value) ? formatNumber(value) : '');
  return `
    <form id="property-form" class="form-grid form-grid--2" novalidate>
      ${inputField({ name: 'title', label: 'Título', value: p.title, full: true, extra: `maxlength="${LIMITS.titleMax}" autofocus` })}
      ${selectFormField('type', 'Tipo de inmueble', PROPERTY_TYPES.map((t) => [t, t]), p.type ?? PROPERTY_TYPES[0])}
      ${selectFormField('operation', 'Operación', Object.entries(OPERATION_LABELS), p.operation ?? 'sale')}
      ${inputField({ name: 'price', label: 'Precio', value: num(p.price), prefix: '$', extra: 'inputmode="numeric" autocomplete="off"', help: p.operation === 'rent' ? 'Canon mensual' : '' })}
      ${selectFormField('status', 'Estado', Object.entries(PROPERTY_STATUS_LABELS), p.status ?? 'available')}
      ${inputField({ name: 'location', label: 'Ubicación', value: p.location, full: true, help: 'Barrio y ciudad, por ejemplo: Laureles, Medellín', extra: `maxlength="${LIMITS.locationMax}"` })}
      ${inputField({ name: 'areaM2', label: 'Área (m²)', value: p.areaM2 ?? '', extra: 'inputmode="decimal"' })}
      ${inputField({ name: 'bedrooms', label: 'Habitaciones', value: p.bedrooms ?? 0, extra: 'inputmode="numeric"' })}
      ${inputField({ name: 'bathrooms', label: 'Baños', value: p.bathrooms ?? 1, extra: 'inputmode="numeric"' })}
      ${inputField({ name: 'parking', label: 'Parqueaderos', value: p.parking ?? 0, extra: 'inputmode="numeric"' })}
      ${inputField({ name: 'imageUrl', label: 'URL de la imagen <span class="field__optional">(opcional)</span>', value: p.imageUrl, type: 'url', full: true, help: 'Si la imagen no carga, se mostrará una imagen genérica.' })}
      <div class="field is-full">
        <label class="field__label" for="pf-description">Descripción <span class="field__optional">(opcional)</span></label>
        <textarea class="field__control" id="pf-description" name="description" maxlength="${LIMITS.descriptionMax}" aria-describedby="pf-description-error">${escapeHTML(p.description ?? '')}</textarea>
        <span class="field__error" id="pf-description-error"></span>
      </div>
    </form>`;
}

function readPropertyForm(form) {
  return Object.fromEntries(new FormData(form).entries());
}

function bindPriceFormatting(form) {
  form.elements.price.addEventListener('blur', (event) => {
    const value = parseNumber(event.target.value);
    if (Number.isFinite(value) && value > 0) event.target.value = formatNumber(value);
  });
}

function submitPropertyForm(modal, propertyId) {
  const form = modal.element.querySelector('#property-form');
  const values = readPropertyForm(form);
  const result = propertyId ? updateProperty(propertyId, values) : createProperty(values, state.user.id);
  if (!result.ok) {
    showFieldErrors(form, result.errors)?.focus();
    enableLiveValidation(form, () => validatePropertyData(normalizePropertyInput(readPropertyForm(form))).errors);
    if (result.errors._form) showToast(result.errors._form, { type: 'error' });
    return;
  }
  modal.close();
  showToast(propertyId ? 'Cambios guardados' : 'Propiedad creada y publicada en el catálogo', { type: 'success' });
}

function openPropertyForm(propertyId = null) {
  const property = propertyId ? getPropertyById(propertyId) : null;
  if (propertyId && !property) {
    showToast('La propiedad ya no existe', { type: 'warning' });
    return;
  }
  const modal = openModal({
    title: property ? 'Editar propiedad' : 'Nueva propiedad',
    body: propertyFormMarkup(property ?? {}),
    footer: `
      <button type="button" class="btn btn--secondary" data-close>Cancelar</button>
      <button type="submit" class="btn btn--primary" form="property-form">Guardar propiedad</button>`,
  });
  const form = modal.element.querySelector('#property-form');
  bindPriceFormatting(form);
  form.addEventListener('submit', (event) => {
    event.preventDefault();
    submitPropertyForm(modal, propertyId);
  });
}

/* ==========================================================================
   Properties: quick actions
   ========================================================================== */

async function handleDeleteProperty(propertyId) {
  const property = getPropertyById(propertyId);
  if (!property) return;
  const linked = countLinkedLeads(propertyId);
  const leadsNote = linked ? ` También se ${linked === 1 ? 'eliminará' : 'eliminarán'} ${escapeHTML(pluralize(linked, 'lead asociado', 'leads asociados'))}.` : '';
  const confirmed = await confirmDialog({
    title: 'Eliminar propiedad',
    messageHTML: `Se eliminará <strong>${escapeHTML(property.title)}</strong>. Esta acción no se puede deshacer.${leadsNote}`,
    confirmLabel: 'Eliminar',
  });
  if (!confirmed) return;
  const result = deleteProperty(propertyId);
  showToast(result.ok ? 'Propiedad eliminada' : result.error, { type: result.ok ? 'success' : 'error' });
}

function handleStatusChange(select) {
  const result = changePropertyStatus(select.dataset.id, select.value);
  if (!result.ok) {
    showToast(result.error, { type: 'error' });
    return;
  }
  showToast(`Estado actualizado: ${PROPERTY_STATUS_LABELS[select.value]}`, { type: 'success' });
}

function bindPropertiesRoot(root) {
  const search = debounce((value) => {
    state.propertyQuery = value;
    renderPropertyTable();
  }, SEARCH_DEBOUNCE_MS);
  root.addEventListener('input', (event) => { if (event.target.id === 'admin-property-search') search(event.target.value); });
  root.addEventListener('change', (event) => { if (event.target.dataset.action === 'status') handleStatusChange(event.target); });
  root.addEventListener('click', (event) => {
    const button = event.target.closest('[data-action]');
    const action = button?.dataset.action;
    if (action === 'new-property') openPropertyForm();
    if (action === 'edit') openPropertyForm(button.dataset.id);
    if (action === 'delete') handleDeleteProperty(button.dataset.id);
    if (action === 'clear-search') {
      const input = document.getElementById('admin-property-search');
      input.value = '';
      state.propertyQuery = '';
      renderPropertyTable();
      input.focus();
    }
  });
}

/* ==========================================================================
   Leads (kanban)
   ========================================================================== */

function leadCardMarkup(lead) {
  const id = escapeHTML(lead.id);
  const clientName = lead.client?.name ?? 'Cliente eliminado';
  return `
    <article class="card lead-card" aria-label="${escapeHTML(clientName)}">
      <div class="lead-card__head">
        <span class="avatar" aria-hidden="true">${escapeHTML(lead.client?.avatar ?? '?')}</span>
        <div class="property-cell__text">
          <span class="lead-card__name">${escapeHTML(clientName)}</span>
          <span class="lead-card__property">${escapeHTML(lead.property?.title ?? 'Propiedad eliminada')}</span>
        </div>
      </div>
      ${badgeMarkup(LEAD_STAGE_LABELS[lead.stage], STATUS_TONES.lead[lead.stage])}
      <p class="lead-card__notes${lead.notes ? '' : ' text-muted'}">${escapeHTML(lead.notes || 'Sin notas comerciales')}</p>
      <div class="lead-card__meta"><span>Actualizado ${escapeHTML(formatRelativeTime(lead.updatedAt ?? lead.createdAt))}</span></div>
      <div class="lead-card__actions">
        <div class="field__select field__select--sm">
          <select class="field__control field__control--sm" data-action="stage" data-id="${id}" data-focus-key="stage-${id}" aria-label="Etapa de ${escapeHTML(clientName)}">
            ${optionsMarkup(STAGE_OPTIONS.map(({ value, label }) => [value, label]), lead.stage)}
          </select>
        </div>
        <button type="button" class="btn btn--secondary btn--sm" data-action="lead-notes" data-id="${id}" data-focus-key="notes-${id}">${iconMarkup('note', 'icon--sm')}Notas</button>
      </div>
    </article>`;
}

function kanbanColumnMarkup(column) {
  const titleId = `kanban-${column.key}`;
  return `
    <section class="kanban__column" aria-labelledby="${titleId}">
      <header class="kanban__header">
        <h2 class="kanban__title" id="${titleId}">${escapeHTML(column.label)}</h2>
        <span class="kanban__count" aria-label="${escapeHTML(pluralize(column.leads.length, 'lead', 'leads'))}">${column.leads.length}</span>
      </header>
      ${column.leads.length ? column.leads.map(leadCardMarkup).join('') : '<p class="kanban__empty">Sin leads en esta etapa</p>'}
    </section>`;
}

function renderLeads() {
  const leads = getLeadsWithDetails();
  if (leads.length === 0) {
    roots.leads().innerHTML = emptyStateMarkup({ icon: 'funnel', title: 'Aún no hay leads', text: 'Cuando un cliente pulse «Me interesa» en una propiedad, aparecerá aquí en la columna Nuevo.' });
    return;
  }
  renderKeepingFocus(roots.leads(), `<div class="kanban">${groupLeadsByStage(leads).map(kanbanColumnMarkup).join('')}</div>`);
}

function leadModalBody(lead) {
  const client = lead.client ?? {};
  const phone = String(client.phone ?? '').replace(/[^\d+]/g, '');
  return `
    <div class="contact-card">
      <span class="avatar" aria-hidden="true">${escapeHTML(client.avatar ?? '?')}</span>
      <span class="contact-card__text">
        <span class="contact-card__label">Interesado en</span>
        <strong>${escapeHTML(lead.property?.title ?? 'Propiedad eliminada')}</strong>
      </span>
      <span class="contact-card__links">
        ${client.email ? `<a class="link-btn" href="mailto:${escapeHTML(client.email)}">${iconMarkup('mail', 'icon--sm')}${escapeHTML(client.email)}</a>` : ''}
        ${phone ? `<a class="link-btn" href="tel:${escapeHTML(phone)}">${iconMarkup('phone', 'icon--sm')}${escapeHTML(client.phone)}</a>` : ''}
      </span>
    </div>
    <form id="lead-notes-form" class="form-grid" novalidate>
      <div class="field">
        <label class="field__label" for="lead-notes">Notas comerciales</label>
        <textarea class="field__control" id="lead-notes" name="notes" maxlength="${LIMITS.notesMax}" aria-describedby="lead-notes-help lead-notes-error" autofocus>${escapeHTML(lead.notes ?? '')}</textarea>
        <span class="field__help" id="lead-notes-help">Máximo ${LIMITS.notesMax} caracteres. Solo las ve el equipo comercial.</span>
        <span class="field__error" id="lead-notes-error"></span>
      </div>
    </form>`;
}

function openLeadNotes(leadId) {
  const lead = getLeadsWithDetails().find((item) => item.id === leadId);
  if (!lead) return;
  const modal = openModal({
    title: lead.client?.name ?? 'Lead',
    body: leadModalBody(lead),
    footer: `
      <button type="button" class="btn btn--secondary" data-close>Cancelar</button>
      <button type="submit" class="btn btn--primary" form="lead-notes-form">Guardar notas</button>`,
  });
  modal.element.querySelector('#lead-notes-form').addEventListener('submit', (event) => {
    event.preventDefault();
    const result = updateLeadNotes(leadId, event.currentTarget.elements.notes.value);
    if (!result.ok) {
      showFieldErrors(event.currentTarget, { notes: result.error })?.focus();
      return;
    }
    modal.close();
    showToast('Notas guardadas', { type: 'success' });
  });
}

function bindLeadsRoot(root) {
  root.addEventListener('change', (event) => {
    if (event.target.dataset.action !== 'stage') return;
    const result = updateLeadStage(event.target.dataset.id, event.target.value);
    showToast(result.ok ? `Lead movido a «${LEAD_STAGE_LABELS[event.target.value]}»` : result.error, { type: result.ok ? 'success' : 'error' });
  });
  root.addEventListener('click', (event) => {
    const button = event.target.closest('[data-action="lead-notes"]');
    if (button && getLeadById(button.dataset.id)) openLeadNotes(button.dataset.id);
  });
}

/* ==========================================================================
   Data tools (export / import / restore)
   ========================================================================== */

const ENTITY_LABELS = { users: ['usuario', 'usuarios'], properties: ['propiedad', 'propiedades'], leads: ['lead', 'leads'] };

function countsMarkup() {
  return ENTITIES.map((entity) => {
    const count = getCollection(entity).length;
    const [one, many] = ENTITY_LABELS[entity];
    return `<span><strong>${formatNumber(count)}</strong> ${count === 1 ? one : many}</span>`;
  }).join('');
}

function toolMarkup({ icon, danger = false, title, text, action }) {
  return `
    <article class="card tool">
      <span class="tool__icon${danger ? ' tool__icon--danger' : ''}">${iconMarkup(icon)}</span>
      <h2>${title}</h2>
      <p class="tool__text">${text}</p>
      ${action}
    </article>`;
}

function renderDataTools() {
  roots.data().innerHTML = `
    <div class="section-stack">
      <p class="stat-list" id="data-counts" aria-live="polite">${countsMarkup()}</p>
      <div class="tool-grid">
        ${toolMarkup({ icon: 'download', title: 'Exportar respaldo', text: 'Descarga un archivo JSON con usuarios, propiedades y leads tal como están ahora.', action: `<button type="button" class="btn btn--secondary" data-action="export">${iconMarkup('download', 'icon--sm')}Exportar JSON</button>` })}
        ${toolMarkup({ icon: 'upload', title: 'Importar respaldo', text: 'Carga un archivo exportado antes. Se valida su estructura antes de aplicarlo.', action: `<input type="file" id="import-file" class="visually-hidden" accept="application/json,.json" tabindex="-1"><button type="button" class="btn btn--secondary" data-action="import">${iconMarkup('upload', 'icon--sm')}Importar JSON</button>` })}
        ${toolMarkup({ icon: 'refresh', danger: true, title: 'Restaurar datos de fábrica', text: 'Vuelve a los datos originales de la carpeta /data/. Se perderán los cambios hechos.', action: `<button type="button" class="btn btn--danger" data-action="reset">${iconMarkup('refresh', 'icon--sm')}Restaurar datos</button>` })}
      </div>
    </div>`;
}

function handleExport() {
  try {
    const fileName = exportBackup();
    showToast(`Respaldo descargado: ${fileName}`, { type: 'success' });
  } catch (error) {
    showToast('No se pudo generar el respaldo. Inténtalo de nuevo', { type: 'error' });
  }
}

async function handleImportFile(input) {
  const file = input.files?.[0];
  input.value = '';
  if (!file) return;
  const confirmed = await confirmDialog({
    title: 'Importar respaldo',
    messageHTML: `Se reemplazarán todos los datos actuales por los del archivo <strong>${escapeHTML(file.name)}</strong>. Esta acción no se puede deshacer.`,
    confirmLabel: 'Importar',
  });
  if (!confirmed) return;
  const result = importBackup(await file.text());
  if (!result.ok) {
    showToast(`No se importó el archivo: ${result.error}`, { type: 'error' });
    return;
  }
  const { properties, leads, users } = result.counts;
  showToast(`Respaldo importado: ${pluralize(properties, 'propiedad', 'propiedades')}, ${pluralize(leads, 'lead', 'leads')} y ${pluralize(users, 'usuario', 'usuarios')}`, { type: 'success' });
}

async function handleReset(button) {
  const confirmed = await confirmDialog({
    title: 'Restaurar datos de fábrica',
    messageHTML: 'Se borrarán todos los cambios y se cargarán de nuevo los datos originales. Esta acción no se puede deshacer.',
    confirmLabel: 'Restaurar',
  });
  if (!confirmed) return;
  setButtonLoading(button, true);
  try {
    await resetToSeed();
    showToast('Datos restaurados a su estado original', { type: 'success' });
  } catch (error) {
    showToast('No se pudieron restaurar los datos. Inténtalo de nuevo', { type: 'error' });
  } finally {
    if (button.isConnected) setButtonLoading(button, false);
  }
}

function bindDataRoot(root) {
  root.addEventListener('click', (event) => {
    const button = event.target.closest('[data-action]');
    if (button?.dataset.action === 'export') handleExport();
    if (button?.dataset.action === 'import') document.getElementById('import-file').click();
    if (button?.dataset.action === 'reset') handleReset(button);
  });
  root.addEventListener('change', (event) => { if (event.target.id === 'import-file') handleImportFile(event.target); });
}

/* ==========================================================================
   Public API
   ========================================================================== */

function skeletonRows(count) {
  return `<div class="section-stack" role="status" aria-label="Cargando">${'<div class="skeleton skeleton--w80" aria-hidden="true"></div>'.repeat(count)}</div>`;
}

/** Loading state shown while seed data is being prepared. */
export function renderAdminLoading() {
  roots.summary().innerHTML = `<div class="kpi-grid" role="status" aria-label="Cargando indicadores">${'<div class="card kpi" aria-hidden="true"><div class="skeleton skeleton--w60"></div><div class="skeleton skeleton--w40"></div></div>'.repeat(5)}</div>`;
  ['properties', 'leads', 'data'].forEach((key) => { roots[key]().innerHTML = skeletonRows(4); });
}

function bindRoots() {
  if (state.bound) return;
  state.bound = true;
  bindPropertiesRoot(roots.properties());
  bindLeadsRoot(roots.leads());
  bindDataRoot(roots.data());
}

export function mountAdminUI(user) {
  state.user = user;
  state.propertyQuery = '';
  bindRoots();
  roots.properties().innerHTML = propertiesShellMarkup();
  renderSummary();
  renderPropertyTable();
  renderLeads();
  renderDataTools();
}

/** Re-renders only the blocks that depend on the changed entity. */
export function refreshAdminUI(entity) {
  if (!state.user) return;
  renderSummary();
  if (['properties', 'all'].includes(entity)) renderPropertyTable();
  if (['leads', 'properties', 'users', 'all'].includes(entity)) renderLeads();
  const counts = document.getElementById('data-counts');
  if (counts) counts.innerHTML = countsMarkup();
}

export function unmountAdminUI() {
  state.user = null;
  Object.values(roots).forEach((root) => { root().innerHTML = ''; });
}
