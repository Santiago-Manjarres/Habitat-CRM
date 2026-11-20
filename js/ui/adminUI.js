/**
 * Advisor panel (the only interface): dashboard (next step, KPIs, funnel chart,
 * recent clients), property CRUD (table + modal form) and client management
 * (kanban, create / edit / delete). In the interface a lead is called a "cliente".
 */
import {
  PROPERTY_TYPES, OPERATION_LABELS, PROPERTY_STATUS_LABELS, LEAD_STAGE_LABELS, LEAD_SOURCE_LABELS,
  STATUS_TONES, SEARCH_DEBOUNCE_MS,
} from '../config.js';
import {
  escapeHTML, iconMarkup, badgeMarkup, imageMarkup, formatCurrency, formatCompactCurrency,
  formatNumber, formatRelativeTime, parseNumber, pluralize, getInitials,
} from '../utils/formatters.js';
import { validatePropertyData, validateLeadData, LIMITS } from '../utils/validators.js';
import {
  getProperties, getPropertyById, filterProperties, createProperty, updateProperty,
  changePropertyStatus, deleteProperty, countLinkedLeads, normalizePropertyInput,
} from '../services/propertyService.js';
import {
  getLeads, getLeadsWithDetails, groupLeadsByStage, updateLeadStage, getLeadById, createLead, updateLead,
  deleteLead, normalizeLeadInput, getDashboardKpis, getFunnelCounts, getRecentLeads,
  STAGE_OPTIONS, SOURCE_OPTIONS,
} from '../services/leadService.js';
import { getAdvisors } from '../services/authService.js';
import { getCollection } from '../services/storageService.js';
import { showToast, openModal, confirmDialog } from './toastUI.js';
import { showFieldErrors, enableLiveValidation } from './authUI.js';

const state = { user: null, propertyQuery: '', bound: false };

const roots = {
  summary: () => document.getElementById('admin-resumen-root'),
  properties: () => document.getElementById('admin-propiedades-root'),
  leads: () => document.getElementById('admin-leads-root'),
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
   Summary: next step, KPIs, funnel chart, inventory by state, recent clients
   ========================================================================== */

function kpiMarkup({ icon, label, value, hint }) {
  return `
    <article class="card kpi">
      <div class="kpi__head">
        <h2 class="kpi__label">${escapeHTML(label)}</h2>
        <span class="kpi__icon">${iconMarkup(icon)}</span>
      </div>
      <p class="kpi__value">${escapeHTML(value)}</p>
      <p class="kpi__hint">${escapeHTML(hint)}</p>
    </article>`;
}

function kpiCards() {
  const k = getDashboardKpis();
  return [
    { icon: 'building', label: 'Propiedades disponibles', value: formatNumber(k.availableProperties), hint: `de ${pluralize(k.totalProperties, 'propiedad', 'propiedades')} en el inventario` },
    { icon: 'sparkle', label: 'Valor del inventario en venta', value: formatCompactCurrency(k.availableInventoryValue), hint: `${formatCurrency(k.availableInventoryValue)} en ${pluralize(k.availableForSaleCount, 'inmueble', 'inmuebles')}` },
    { icon: 'user', label: 'Clientes en seguimiento', value: formatNumber(k.activeLeads), hint: k.newLeads ? `${pluralize(k.newLeads, 'nuevo', 'nuevos')} por contactar` : 'Todos contactados' },
    { icon: 'check-circle', label: 'Tasa de cierre', value: k.winRate === null ? '—' : `${k.winRate} %`, hint: k.closedDeals ? `${pluralize(k.wonDeals, 'negocio cerrado', 'negocios cerrados')} de ${pluralize(k.closedDeals, 'terminado', 'terminados')}` : 'Aún no hay negocios terminados' },
  ];
}

/** Vertical bars per funnel stage (SVG; colours come from CSS classes). */
function funnelChartMarkup() {
  const data = getFunnelCounts();
  const max = Math.max(1, ...data.map((d) => d.count));
  const topIndex = data.findIndex((d) => d.count === max);
  const [W, H, BAR, TOP] = [500, 200, 56, 24];
  const step = W / data.length;
  const bars = data.map((d, i) => {
    const h = Math.round(((H - TOP - 28) * d.count) / max);
    const x = Math.round(i * step + (step - BAR) / 2);
    const y = H - 28 - h;
    const top = i === topIndex && d.count > 0 ? ' chart__bar--top' : '';
    return `<rect class="chart__track" x="${x}" y="${TOP}" width="${BAR}" height="${H - 28 - TOP}" rx="10"></rect>
      <rect class="chart__bar${top}" x="${x}" y="${y}" width="${BAR}" height="${Math.max(h, 0)}" rx="10"></rect>
      <text class="chart__value" x="${x + BAR / 2}" y="${Math.max(y - 8, 14)}" text-anchor="middle">${d.count}</text>
      <text class="chart__label" x="${x + BAR / 2}" y="${H - 6}" text-anchor="middle">${escapeHTML(d.label)}</text>`;
  }).join('');
  const summary = data.map((d) => `${d.label}: ${d.count}`).join(', ');
  return `<svg class="chart" viewBox="0 0 ${W} ${H}" role="img" aria-label="Clientes por etapa. ${escapeHTML(summary)}">${bars}</svg>`;
}

function inventoryMarkup() {
  const properties = getCollection('properties');
  const rows = Object.entries(PROPERTY_STATUS_LABELS).map(([status, label]) => {
    const count = properties.filter((p) => p.status === status).length;
    const pct = properties.length ? Math.round((count / properties.length) * 100) : 0;
    return `
      <li class="meter">
        <span class="meter__top">${badgeMarkup(label, STATUS_TONES.property[status])}<strong class="tabular">${count}</strong></span>
        <svg class="meter__bar" viewBox="0 0 100 6" preserveAspectRatio="none" aria-hidden="true"><rect class="meter__track" width="100" height="6"></rect><rect class="meter__fill meter__fill--${STATUS_TONES.property[status]}" width="${pct}" height="6"></rect></svg>
      </li>`;
  }).join('');
  return `<ul class="meter-list" role="list">${rows}</ul>`;
}

function recentLeadRow(lead) {
  return `
    <tr>
      <td class="is-full">
        <span class="contact-cell">
          <span class="avatar avatar--sm" aria-hidden="true">${escapeHTML(getInitials(lead.contactName))}</span>
          <span class="property-cell__text">
            <span class="property-cell__title">${escapeHTML(lead.contactName)}</span>
            <span class="text-sm text-muted">${escapeHTML(LEAD_SOURCE_LABELS[lead.source] ?? '—')}</span>
          </span>
        </span>
      </td>
      <td data-label="Propiedad">${escapeHTML(lead.property?.title ?? 'Propiedad eliminada')}</td>
      <td data-label="Asesor">${escapeHTML(lead.agent?.name ?? '—')}</td>
      <td data-label="Etapa">${badgeMarkup(LEAD_STAGE_LABELS[lead.stage], STATUS_TONES.lead[lead.stage])}</td>
      <td data-label="Actualizado" class="text-muted">${escapeHTML(formatRelativeTime(lead.updatedAt ?? lead.createdAt))}</td>
      <td class="is-full"><span class="data-table__actions"><button type="button" class="btn btn--ghost btn--sm" data-action="open-lead" data-id="${escapeHTML(lead.id)}">Ver ficha</button></span></td>
    </tr>`;
}

function recentLeadsMarkup() {
  const leads = getRecentLeads(5);
  if (!leads.length) return '<p class="text-muted">Aún no hay clientes. Registra el primero con «Nuevo cliente».</p>';
  return `
    <table class="data-table">
      <caption class="visually-hidden">Clientes con actividad reciente</caption>
      <thead><tr><th scope="col">Cliente</th><th scope="col">Propiedad</th><th scope="col">Asesor</th><th scope="col">Etapa</th><th scope="col">Actualizado</th><th scope="col"><span class="visually-hidden">Acciones</span></th></tr></thead>
      <tbody>${leads.map(recentLeadRow).join('')}</tbody>
    </table>`;
}

function panelMarkup(title, body, { action = '', className = '' } = {}) {
  return `
    <section class="card panel ${className}">
      <header class="panel__header"><h2 class="panel__title">${title}</h2>${action}</header>
      ${body}
    </section>`;
}

/** The one thing to do today: clients nobody has called yet. */
function nextStepMarkup() {
  const pending = getLeads().filter((lead) => lead.stage === 'new').length;
  const title = pending
    ? `Tienes ${pluralize(pending, 'cliente nuevo', 'clientes nuevos')} por contactar`
    : 'Todos los clientes nuevos ya fueron contactados';
  const hint = pending
    ? 'Llámalos o escríbeles, y luego cambia su etapa a «Contactado».'
    : 'Cuando llegue alguien interesado, regístralo con «Nuevo cliente».';
  return `
    <section class="next-step" aria-label="Siguiente paso">
      <span class="next-step__icon">${iconMarkup(pending ? 'phone' : 'check-circle')}</span>
      <span class="next-step__text"><span class="next-step__title">${escapeHTML(title)}</span><span class="next-step__hint">${escapeHTML(hint)}</span></span>
      ${pending ? '<a class="btn btn--primary btn--sm" href="#/clientes">Ver clientes por contactar</a>' : ''}
    </section>`;
}

function renderSummary() {
  roots.summary().innerHTML = `
    <div class="section-stack">
      ${nextStepMarkup()}
      <div class="kpi-grid">${kpiCards().map(kpiMarkup).join('')}</div>
      <div class="dash-grid">
        ${panelMarkup('Clientes por etapa', funnelChartMarkup(), { className: 'panel--chart', action: '<a class="link-btn" href="#/clientes">Ver clientes</a>' })}
        ${panelMarkup('Inventario por estado', inventoryMarkup(), { action: '<a class="link-btn" href="#/propiedades">Ver propiedades</a>' })}
      </div>
      ${panelMarkup('Últimos movimientos', recentLeadsMarkup(), { className: 'panel--table', action: '<a class="link-btn" href="#/clientes">Ver todos</a>' })}
    </div>`;
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
    container.innerHTML = emptyStateMarkup({ icon: 'building', title: 'Aún no hay propiedades', text: 'Registra la primera para empezar a asociarle clientes interesados.', action: `<button type="button" class="btn btn--primary" data-action="new-property">${iconMarkup('plus')}Crear propiedad</button>` });
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
      ${inputField({ name: 'imageUrl', label: 'Enlace de la foto <span class="field__optional">(opcional)</span>', value: p.imageUrl, type: 'url', full: true, help: 'Copia y pega el enlace de una foto publicada en internet. Si no carga, se muestra una imagen genérica.' })}
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
  showToast(propertyId ? 'Cambios guardados' : 'Propiedad creada', { type: 'success' });
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
  const leadsNote = linked ? ` También se ${linked === 1 ? 'eliminará' : 'eliminarán'} ${escapeHTML(pluralize(linked, 'cliente interesado', 'clientes interesados'))}.` : '';
  const confirmed = await confirmDialog({
    title: 'Eliminar propiedad',
    messageHTML: `Se eliminará <strong>${escapeHTML(property.title)}</strong>.${leadsNote} Esta acción no se puede deshacer.`,
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
   Clients (leads): kanban + create / edit / delete
   ========================================================================== */

function contactLinksMarkup(lead) {
  const phone = String(lead.contactPhone ?? '').replace(/[^\d+]/g, '');
  const links = [
    phone ? `<a class="link-btn" href="tel:${escapeHTML(phone)}">${iconMarkup('phone', 'icon--sm')}${escapeHTML(lead.contactPhone)}</a>` : '',
    lead.contactEmail ? `<a class="link-btn" href="mailto:${escapeHTML(lead.contactEmail)}">${iconMarkup('mail', 'icon--sm')}${escapeHTML(lead.contactEmail)}</a>` : '',
  ].filter(Boolean);
  return links.join('');
}

function leadCardMarkup(lead) {
  const id = escapeHTML(lead.id);
  return `
    <article class="card lead-card" aria-label="${escapeHTML(lead.contactName)}">
      <div class="lead-card__head">
        <span class="avatar" aria-hidden="true">${escapeHTML(getInitials(lead.contactName))}</span>
        <div class="property-cell__text">
          <span class="lead-card__name">${escapeHTML(lead.contactName)}</span>
          <span class="lead-card__property">${escapeHTML(lead.property?.title ?? 'Propiedad eliminada')}</span>
        </div>
      </div>
      ${badgeMarkup(LEAD_STAGE_LABELS[lead.stage], STATUS_TONES.lead[lead.stage])}
      <p class="lead-card__notes${lead.notes ? '' : ' text-muted'}">${escapeHTML(lead.notes || 'Sin notas')}</p>
      <div class="lead-card__meta">
        <span>${escapeHTML(LEAD_SOURCE_LABELS[lead.source] ?? '—')} · ${escapeHTML(formatRelativeTime(lead.updatedAt ?? lead.createdAt))}</span>
        <span class="avatar avatar--xs" title="${escapeHTML(lead.agent?.name ?? '')}" aria-label="Asesor: ${escapeHTML(lead.agent?.name ?? 'sin asignar')}">${escapeHTML(lead.agent?.avatar ?? '?')}</span>
      </div>
      <div class="lead-card__actions">
        <div class="lead-card__stage">
          <label class="field__label" for="stage-${id}">Cambiar etapa</label>
          <div class="field__select field__select--sm">
            <select class="field__control field__control--sm" id="stage-${id}" data-action="stage" data-id="${id}" data-focus-key="stage-${id}">
              ${optionsMarkup(STAGE_OPTIONS.map(({ value, label }) => [value, label]), lead.stage)}
            </select>
          </div>
        </div>
        <button type="button" class="btn btn--secondary btn--sm" data-action="open-lead" data-id="${id}" data-focus-key="open-${id}">${iconMarkup('note', 'icon--sm')}Ver ficha</button>
      </div>
    </article>`;
}

function kanbanColumnMarkup(column) {
  const titleId = `kanban-${column.key}`;
  return `
    <section class="kanban__column" aria-labelledby="${titleId}">
      <header class="kanban__header">
        <h2 class="kanban__title" id="${titleId}">${escapeHTML(column.label)}</h2>
        <span class="kanban__count" aria-label="${escapeHTML(pluralize(column.leads.length, 'cliente', 'clientes'))}">${column.leads.length}</span>
      </header>
      ${column.leads.length ? column.leads.map(leadCardMarkup).join('') : '<p class="kanban__empty">Nadie en esta etapa</p>'}
    </section>`;
}

function renderLeads() {
  const leads = getLeadsWithDetails();
  if (leads.length === 0) {
    roots.leads().innerHTML = emptyStateMarkup({ icon: 'funnel', title: 'Aún no hay clientes', text: 'Cada persona que llame, escriba o visite la oficina por una propiedad se registra aquí para seguirla hasta el cierre.', action: `<button type="button" class="btn btn--primary" data-action="new-lead">${iconMarkup('plus')}Registrar el primer cliente</button>` });
    return;
  }
  renderKeepingFocus(roots.leads(), `<div class="kanban">${groupLeadsByStage(leads).map(kanbanColumnMarkup).join('')}</div>`);
}

/* ---------- Lead form (create / edit) ---------- */

function leadField({ name, label, value = '', type = 'text', full = false, help = '', extra = '' }) {
  const id = `lf-${name}`;
  return `
    <div class="field${full ? ' is-full' : ''}">
      <label class="field__label" for="${id}">${label}</label>
      <input class="field__control" id="${id}" name="${name}" type="${type}" value="${escapeHTML(value)}" aria-describedby="${help ? `${id}-help ` : ''}${id}-error" ${extra}>
      ${help ? `<span class="field__help" id="${id}-help">${help}</span>` : ''}
      <span class="field__error" id="${id}-error"></span>
    </div>`;
}

function leadSelect(name, label, entries, selected, full = false) {
  const id = `lf-${name}`;
  return `
    <div class="field${full ? ' is-full' : ''}">
      <label class="field__label" for="${id}">${label}</label>
      <div class="field__select"><select class="field__control" id="${id}" name="${name}" aria-describedby="${id}-error">${optionsMarkup(entries, selected)}</select></div>
      <span class="field__error" id="${id}-error"></span>
    </div>`;
}

function propertyOptions(currentId) {
  return getProperties()
    .filter((p) => p.status !== 'sold' || p.id === currentId)
    .map((p) => [p.id, `${p.title} · ${formatCurrency(p.price, p.currency)}`]);
}

function leadFormMarkup(lead = {}) {
  const advisors = getAdvisors().map((a) => [a.id, a.name]);
  return `
    ${lead.id ? `<div class="contact-card"><span class="contact-card__text"><span class="contact-card__label">Contactar</span></span><span class="contact-card__links">${contactLinksMarkup(lead) || '<span class="text-muted">Sin datos de contacto</span>'}</span></div>` : ''}
    <form id="lead-form" class="form-grid form-grid--2" novalidate>
      ${leadField({ name: 'contactName', label: 'Nombre del cliente', value: lead.contactName, full: true, extra: `maxlength="${LIMITS.nameMax}" autocomplete="off" autofocus` })}
      ${leadField({ name: 'contactPhone', label: 'Teléfono', value: lead.contactPhone, type: 'tel', extra: 'inputmode="tel" autocomplete="off"', help: 'Teléfono o correo: al menos uno.' })}
      ${leadField({ name: 'contactEmail', label: 'Correo', value: lead.contactEmail, type: 'email', extra: 'inputmode="email" autocomplete="off"' })}
      ${leadSelect('propertyId', 'Propiedad de interés', propertyOptions(lead.propertyId), lead.propertyId, true)}
      ${leadSelect('source', '¿Cómo llegó?', SOURCE_OPTIONS.map(({ value, label }) => [value, label]), lead.source ?? 'phone')}
      ${leadSelect('stage', 'Etapa', STAGE_OPTIONS.map(({ value, label }) => [value, label]), lead.stage ?? 'new')}
      ${leadSelect('agentId', 'Asesor responsable', advisors, lead.agentId ?? state.user.id, true)}
      <div class="field is-full">
        <label class="field__label" for="lf-notes">Notas <span class="field__optional">(opcional)</span></label>
        <textarea class="field__control" id="lf-notes" name="notes" maxlength="${LIMITS.notesMax}" aria-describedby="lf-notes-error">${escapeHTML(lead.notes ?? '')}</textarea>
        <span class="field__error" id="lf-notes-error"></span>
      </div>
    </form>`;
}

function leadFooterMarkup(isEdit) {
  return `
    ${isEdit ? '<button type="button" class="btn btn--ghost modal__footer-start" data-action="delete-lead">' + iconMarkup('trash', 'icon--sm') + 'Eliminar</button>' : ''}
    <button type="button" class="btn btn--secondary" data-close>Cancelar</button>
    <button type="submit" class="btn btn--primary" form="lead-form">${isEdit ? 'Guardar cambios' : 'Guardar cliente'}</button>`;
}

function submitLeadForm(modal, leadId) {
  const form = modal.element.querySelector('#lead-form');
  const values = Object.fromEntries(new FormData(form).entries());
  const result = leadId ? updateLead(leadId, values) : createLead(values);
  if (!result.ok) {
    showFieldErrors(form, result.errors)?.focus();
    enableLiveValidation(form, () => validateLeadData(normalizeLeadInput(Object.fromEntries(new FormData(form).entries()))).errors);
    if (result.errors._form) showToast(result.errors._form, { type: 'error' });
    return;
  }
  modal.close();
  showToast(leadId ? 'Cambios guardados' : `Cliente registrado: ${result.lead.contactName}`, { type: 'success' });
}

async function handleDeleteLead(modal, lead) {
  const confirmed = await confirmDialog({
    title: 'Eliminar cliente',
    messageHTML: `Se eliminará la ficha de <strong>${escapeHTML(lead.contactName)}</strong>. Esta acción no se puede deshacer.`,
    confirmLabel: 'Eliminar',
  });
  if (!confirmed) return;
  const result = deleteLead(lead.id);
  modal.close();
  showToast(result.ok ? 'Cliente eliminado' : result.error, { type: result.ok ? 'success' : 'error' });
}

function openLeadForm(leadId = null) {
  const lead = leadId ? getLeadById(leadId) : null;
  if (leadId && !lead) {
    showToast('Este cliente ya no existe', { type: 'warning' });
    return;
  }
  const modal = openModal({ title: lead ? lead.contactName : 'Nuevo cliente', body: leadFormMarkup(lead ?? {}), footer: leadFooterMarkup(Boolean(lead)) });
  modal.element.querySelector('#lead-form').addEventListener('submit', (event) => {
    event.preventDefault();
    submitLeadForm(modal, leadId);
  });
  modal.element.querySelector('[data-action="delete-lead"]')?.addEventListener('click', () => handleDeleteLead(modal, lead));
}

function bindLeadsRoot(root) {
  root.addEventListener('change', (event) => {
    if (event.target.dataset.action !== 'stage') return;
    const result = updateLeadStage(event.target.dataset.id, event.target.value);
    showToast(result.ok ? `Etapa cambiada a «${LEAD_STAGE_LABELS[event.target.value]}»` : result.error, { type: result.ok ? 'success' : 'error' });
  });
}

/* ==========================================================================
   Public API
   ========================================================================== */

function skeletonRows(count) {
  return `<div class="section-stack" role="status" aria-label="Cargando">${'<div class="skeleton skeleton--w80" aria-hidden="true"></div>'.repeat(count)}</div>`;
}

/** Loading state shown while seed data is being prepared. */
export function renderAdminLoading() {
  roots.summary().innerHTML = `<div class="kpi-grid" role="status" aria-label="Cargando indicadores">${'<div class="card kpi" aria-hidden="true"><div class="skeleton skeleton--w60"></div><div class="skeleton skeleton--w40"></div></div>'.repeat(4)}</div>`;
  ['properties', 'leads'].forEach((key) => { roots[key]().innerHTML = skeletonRows(4); });
}

/** One delegated listener for the actions that live in several sections. */
function bindViewActions() {
  document.getElementById('view-admin').addEventListener('click', (event) => {
    const button = event.target.closest('[data-action="new-lead"], [data-action="open-lead"]');
    if (!button) return;
    openLeadForm(button.dataset.action === 'open-lead' ? button.dataset.id : null);
  });
}

function bindRoots() {
  if (state.bound) return;
  state.bound = true;
  bindViewActions();
  bindPropertiesRoot(roots.properties());
  bindLeadsRoot(roots.leads());
}

function renderToday() {
  const today = document.getElementById('topbar-date');
  if (!today) return;
  const text = new Intl.DateTimeFormat('es-CO', { weekday: 'long', day: 'numeric', month: 'long' }).format(new Date());
  today.textContent = text.charAt(0).toUpperCase() + text.slice(1);
}

export function mountAdminUI(user) {
  state.user = user;
  state.propertyQuery = '';
  bindRoots();
  renderToday();
  roots.properties().innerHTML = propertiesShellMarkup();
  renderSummary();
  renderPropertyTable();
  renderLeads();
}

/** Re-renders only the blocks that depend on the changed entity. */
export function refreshAdminUI(entity) {
  if (!state.user) return;
  renderSummary();
  if (['properties', 'all'].includes(entity)) renderPropertyTable();
  if (['leads', 'properties', 'users', 'all'].includes(entity)) renderLeads();
}

export function unmountAdminUI() {
  state.user = null;
  Object.values(roots).forEach((root) => { root().innerHTML = ''; });
}
