/**
 * Client portal: catalog with real-time filters, property detail (modal) with
 * "Me interesa / Consultar" and the "Mis consultas" list.
 */
import {
  PROPERTY_TYPES, OPERATION_LABELS, PROPERTY_STATUS_LABELS, LEAD_STAGE_LABELS,
  STATUS_TONES, SEARCH_DEBOUNCE_MS,
} from '../config.js';
import {
  escapeHTML, iconMarkup, badgeMarkup, imageMarkup, formatCurrency, formatArea,
  formatNumber, formatRelativeTime, parseNumber, pluralize,
} from '../utils/formatters.js';
import { getProperties, getPropertyById, filterProperties, hasActiveFilters, EMPTY_FILTERS } from '../services/propertyService.js';
import { expressInterest, findActiveLead, getLeadsByClient, isActiveLead } from '../services/leadService.js';
import { getUserById } from '../services/authService.js';
import { showToast, openModal } from './toastUI.js';
import { setButtonLoading } from './authUI.js';

const state = { user: null, filters: { ...EMPTY_FILTERS }, detail: null, bound: false };

const roots = {
  catalog: () => document.getElementById('client-catalog-root'),
  inquiries: () => document.getElementById('client-inquiries-root'),
};

/* ==========================================================================
   Shared markup
   ========================================================================== */

function priceMarkup(property) {
  const suffix = property.operation === 'rent' ? ' <span class="price__suffix">/ mes</span>' : '';
  return `<span class="price">${escapeHTML(formatCurrency(property.price, property.currency))}${suffix}</span>`;
}

function propertyBadges(property) {
  return badgeMarkup(OPERATION_LABELS[property.operation], 'neutral', { plain: true })
    + badgeMarkup(PROPERTY_STATUS_LABELS[property.status], STATUS_TONES.property[property.status]);
}

function featuresMarkup(property, { long = false } = {}) {
  const items = [['area', formatArea(property.areaM2), 'Área']];
  if (property.bedrooms > 0) items.push(['bed', long ? pluralize(property.bedrooms, 'habitación', 'habitaciones') : formatNumber(property.bedrooms), 'Habitaciones']);
  items.push(['bath', long ? pluralize(property.bathrooms, 'baño', 'baños') : formatNumber(property.bathrooms), 'Baños']);
  if (property.parking > 0) items.push(['car', long ? pluralize(property.parking, 'parqueadero', 'parqueaderos') : formatNumber(property.parking), 'Parqueaderos']);
  return `<span class="features">${items.map(([icon, value, label]) => `
    <span class="features__item">${iconMarkup(icon, 'icon--sm')}<span class="visually-hidden">${label}:</span> ${escapeHTML(value)}</span>`).join('')}</span>`;
}

function locationMarkup(location) {
  return `<span class="location">${iconMarkup('pin', 'icon--sm')}<span>${escapeHTML(location)}</span></span>`;
}

/* ==========================================================================
   Catalog: filters
   ========================================================================== */

function optionsMarkup(entries, allLabel) {
  return `<option value="">${allLabel}</option>${entries.map(([value, label]) => `<option value="${escapeHTML(value)}">${escapeHTML(label)}</option>`).join('')}`;
}

function selectField(id, name, label, options) {
  return `
    <div class="field">
      <label class="field__label" for="${id}">${label}</label>
      <div class="field__select"><select class="field__control" id="${id}" name="${name}">${options}</select></div>
    </div>`;
}

function filterBarMarkup() {
  return `
    <form class="card filter-bar" id="catalog-filters" role="search" aria-label="Filtrar propiedades" novalidate>
      <div class="filter-bar__fields">
        <div class="field filter-bar__search search-field">
          <label class="field__label" for="filter-query">Buscar</label>
          <div class="field__affix field__affix--prefix">
            <span class="field__prefix">${iconMarkup('search', 'icon--sm')}</span>
            <input class="field__control" id="filter-query" name="query" type="search" placeholder="Título o ubicación" autocomplete="off">
          </div>
        </div>
        ${selectField('filter-type', 'type', 'Tipo', optionsMarkup(PROPERTY_TYPES.map((t) => [t, t]), 'Todos'))}
        ${selectField('filter-operation', 'operation', 'Operación', optionsMarkup(Object.entries(OPERATION_LABELS), 'Todas'))}
        ${selectField('filter-status', 'status', 'Estado', optionsMarkup(Object.entries(PROPERTY_STATUS_LABELS), 'Todos'))}
        <div class="field">
          <label class="field__label" for="filter-max-price">Precio máximo</label>
          <div class="field__affix field__affix--prefix">
            <span class="field__prefix">$</span>
            <input class="field__control" id="filter-max-price" name="maxPrice" type="text" inputmode="numeric" placeholder="Sin límite" autocomplete="off">
          </div>
        </div>
      </div>
      <div class="filter-bar__footer">
        <p class="results-count" id="catalog-count" aria-live="polite"></p>
        <button type="reset" class="btn btn--ghost btn--sm" id="catalog-clear">Limpiar filtros</button>
      </div>
    </form>`;
}

function readFilters(form) {
  const values = Object.fromEntries(new FormData(form).entries());
  const maxPrice = parseNumber(values.maxPrice);
  return { ...values, maxPrice: Number.isFinite(maxPrice) && maxPrice > 0 ? maxPrice : null };
}

function debounce(fn, wait) {
  let handle = null;
  return (...args) => {
    clearTimeout(handle);
    handle = setTimeout(() => fn(...args), wait);
  };
}

function bindFilters(form) {
  const apply = () => {
    state.filters = readFilters(form);
    renderCatalogResults();
  };
  const applyDebounced = debounce(apply, SEARCH_DEBOUNCE_MS);
  form.addEventListener('input', (event) => (event.target.tagName === 'SELECT' ? apply() : applyDebounced()));
  form.addEventListener('change', (event) => { if (event.target.tagName === 'SELECT') apply(); });
  form.addEventListener('submit', (event) => event.preventDefault());
  form.addEventListener('reset', () => setTimeout(apply));
  form.elements.maxPrice.addEventListener('blur', (event) => {
    const value = parseNumber(event.target.value);
    event.target.value = Number.isFinite(value) && value > 0 ? formatNumber(value) : '';
  });
}

/* ==========================================================================
   Catalog: results
   ========================================================================== */

function propertyCardMarkup(property) {
  return `
    <button type="button" class="property-card" data-property-id="${escapeHTML(property.id)}">
      <span class="property-card__media">
        ${imageMarkup(property.imageUrl)}
        <span class="property-card__badges">${propertyBadges(property)}</span>
      </span>
      <span class="property-card__body">
        ${priceMarkup(property)}
        <span class="property-card__title">${escapeHTML(property.title)}</span>
        ${locationMarkup(property.location)}
        ${featuresMarkup(property)}
      </span>
    </button>`;
}

function emptyStateMarkup({ icon, title, text, action = '' }) {
  return `
    <div class="card empty-state">
      ${iconMarkup(icon, 'empty-state__icon')}
      <h2 class="empty-state__title">${escapeHTML(title)}</h2>
      <p class="empty-state__text">${escapeHTML(text)}</p>
      ${action}
    </div>`;
}

function catalogEmptyMarkup(totalCount) {
  if (totalCount === 0) {
    return emptyStateMarkup({ icon: 'building', title: 'Aún no hay propiedades publicadas', text: 'Vuelve pronto: el equipo está preparando nuevos inmuebles.' });
  }
  return emptyStateMarkup({
    icon: 'search',
    title: 'No encontramos propiedades con esos filtros',
    text: 'Prueba con otra ubicación, sube el precio máximo o quita algún filtro.',
    action: '<button type="button" class="btn btn--secondary" data-action="clear-filters">Limpiar filtros</button>',
  });
}

/** Re-renders a container keeping keyboard focus on the same card if it still exists. */
function renderKeepingFocus(container, html) {
  const focusedId = container.contains(document.activeElement) ? document.activeElement.dataset.propertyId : null;
  container.innerHTML = html;
  if (focusedId) container.querySelector(`[data-property-id="${CSS.escape(focusedId)}"]`)?.focus();
}

/** Re-renders only the results grid (filters keep their values and focus). */
function renderCatalogResults({ intro = false } = {}) {
  const container = document.getElementById('catalog-results');
  if (!container) return;
  const all = getProperties();
  const results = filterProperties(all, state.filters);
  document.getElementById('catalog-count').textContent = pluralize(results.length, 'propiedad', 'propiedades');
  document.getElementById('catalog-clear').hidden = !hasActiveFilters(state.filters);
  renderKeepingFocus(container, results.length
    ? `<div class="property-grid${intro ? ' property-grid--intro' : ''}">${results.map(propertyCardMarkup).join('')}</div>`
    : catalogEmptyMarkup(all.length));
}

function skeletonCardsMarkup(count) {
  const card = `
    <div class="skeleton-card" aria-hidden="true">
      <div class="skeleton"></div>
      <div class="skeleton skeleton--w40"></div>
      <div class="skeleton skeleton--w80"></div>
      <div class="skeleton skeleton--w60"></div>
    </div>`;
  return `<div class="property-grid" role="status" aria-label="Cargando propiedades">${card.repeat(count)}</div>`;
}

/** Loading state shown while seed data is being prepared. */
export function renderClientLoading() {
  roots.catalog().innerHTML = skeletonCardsMarkup(6);
  roots.inquiries().innerHTML = '';
}

/* ==========================================================================
   Property detail (modal)
   ========================================================================== */

function agentMarkup(agent) {
  if (!agent) return '';
  const phone = String(agent.phone ?? '').replace(/[^\d+]/g, '');
  return `
    <div class="contact-card">
      <span class="avatar" aria-hidden="true">${escapeHTML(agent.avatar)}</span>
      <span class="contact-card__text">
        <span class="contact-card__label">Asesor responsable</span>
        <strong>${escapeHTML(agent.name)}</strong>
      </span>
      <span class="contact-card__links">
        ${agent.email ? `<a class="link-btn" href="mailto:${escapeHTML(agent.email)}">${iconMarkup('mail', 'icon--sm')}${escapeHTML(agent.email)}</a>` : ''}
        ${phone ? `<a class="link-btn" href="tel:${escapeHTML(phone)}">${iconMarkup('phone', 'icon--sm')}${escapeHTML(agent.phone)}</a>` : ''}
      </span>
    </div>`;
}

function detailBodyMarkup(property) {
  return `
    <div class="detail__media">${imageMarkup(property.imageUrl, { lazy: false, width: 1280, height: 800 })}</div>
    <div class="detail__summary">
      <div class="detail__badges">${propertyBadges(property)}${badgeMarkup(property.type, 'neutral', { plain: true })}</div>
      ${priceMarkup(property)}
      ${locationMarkup(property.location)}
      ${featuresMarkup(property, { long: true })}
    </div>
    ${property.description ? `<p class="detail__description">${escapeHTML(property.description)}</p>` : ''}
    ${agentMarkup(getUserById(property.agentId))}`;
}

function detailFooterMarkup(property) {
  const lead = findActiveLead(state.user.id, property.id);
  if (lead) {
    return `
      <p class="text-sm text-muted" role="status">Ya enviaste una consulta · Etapa: <strong>${escapeHTML(LEAD_STAGE_LABELS[lead.stage])}</strong></p>
      <a class="btn btn--secondary" href="#/consultas" data-close>Ver mis consultas</a>`;
  }
  if (property.status === 'sold') {
    return `
      <p class="text-sm text-muted">Esta propiedad ya fue vendida o arrendada.</p>
      <button type="button" class="btn btn--primary" disabled>No disponible</button>`;
  }
  return `
    <button type="button" class="btn btn--secondary" data-close>Cerrar</button>
    <button type="button" class="btn btn--primary" data-action="interest">${iconMarkup('heart')}Me interesa / Consultar</button>`;
}

function handleInterest(button) {
  const { propertyId } = state.detail;
  setButtonLoading(button, true);
  const result = expressInterest(state.user, propertyId);
  setButtonLoading(button, false);
  if (!result.ok) {
    showToast(result.error, { type: 'error' });
    return;
  }
  const agent = getUserById(result.lead.agentId);
  const agentName = agent ? agent.name.replace(/\s*\(.*\)$/, '') : 'Un asesor';
  showToast(result.created ? `Consulta enviada. ${agentName} te contactará pronto` : 'Ya tenías una consulta activa para esta propiedad', { type: 'success' });
  updateDetail();
  state.detail?.modal.element.querySelector('.modal__footer .btn')?.focus();
}

function bindDetail(modal) {
  modal.element.addEventListener('click', (event) => {
    const button = event.target.closest('[data-action="interest"]');
    if (button) handleInterest(button);
  });
}

function openPropertyDetail(propertyId) {
  const property = getPropertyById(propertyId);
  if (!property) {
    showToast('Esta propiedad ya no está publicada', { type: 'warning' });
    return;
  }
  const modal = openModal({
    title: property.title,
    body: detailBodyMarkup(property),
    footer: detailFooterMarkup(property),
    onClose: () => { state.detail = null; },
  });
  state.detail = { modal, propertyId };
  bindDetail(modal);
}

/** Refreshes the open detail after changes (own or from another tab). */
function updateDetail() {
  if (!state.detail) return;
  const property = getPropertyById(state.detail.propertyId);
  if (!property) {
    state.detail.modal.close();
    showToast('La propiedad que estabas viendo ya no está publicada', { type: 'warning' });
    return;
  }
  const { element } = state.detail.modal;
  const parts = [['.modal__body', detailBodyMarkup(property)], ['.modal__footer', detailFooterMarkup(property)]];
  element.querySelector('.modal__title').textContent = property.title;
  parts.forEach(([selector, html]) => {
    const part = element.querySelector(selector);
    if (part.dataset.html === html) return;
    const hadFocus = part.contains(document.activeElement);
    part.innerHTML = html;
    part.dataset.html = html;
    if (hadFocus) element.querySelector('[data-close]')?.focus();
  });
}

/* ==========================================================================
   Mis consultas
   ========================================================================== */

function inquiryMarkup(lead) {
  const property = lead.property;
  const agentName = lead.agent?.name ?? 'Sin asignar';
  return `
    <button type="button" class="card inquiry" data-property-id="${escapeHTML(property.id)}">
      ${imageMarkup(property.imageUrl, { className: 'inquiry__thumb', width: 96, height: 72 })}
      <span class="inquiry__body">
        <span class="inquiry__top">
          <span class="inquiry__title">${escapeHTML(property.title)}</span>
          ${badgeMarkup(LEAD_STAGE_LABELS[lead.stage], STATUS_TONES.lead[lead.stage])}
        </span>
        <span class="inquiry__meta">${escapeHTML(property.location)} · ${escapeHTML(formatCurrency(property.price, property.currency))}</span>
        <span class="inquiry__meta">Consultaste ${escapeHTML(formatRelativeTime(lead.createdAt))} · Asesor: ${escapeHTML(agentName)}</span>
      </span>
    </button>`;
}

function renderInquiries() {
  const root = roots.inquiries();
  const leads = getLeadsByClient(state.user.id).filter((lead) => lead.property);
  updateInquiryCount(leads.filter(isActiveLead).length);
  if (leads.length === 0) {
    root.innerHTML = emptyStateMarkup({
      icon: 'inbox',
      title: 'Aún no tienes consultas',
      text: 'Explora el catálogo y pulsa «Me interesa» en el inmueble que te guste. Tu asesor te contactará.',
      action: '<a class="btn btn--primary" href="#/catalogo">Explorar el catálogo</a>',
    });
    return;
  }
  renderKeepingFocus(root, `<div class="inquiry-list">${leads.map(inquiryMarkup).join('')}</div>`);
}

function updateInquiryCount(count) {
  document.querySelectorAll('#view-client [data-route="consultas"]').forEach((link) => {
    link.querySelector('.nav-count-wrap')?.remove();
    if (count === 0) return;
    link.insertAdjacentHTML('beforeend', `<span class="nav-count-wrap"><span class="badge badge--count nav-count" aria-hidden="true">${count}</span><span class="visually-hidden">, ${escapeHTML(pluralize(count, 'consulta activa', 'consultas activas'))}</span></span>`);
  });
}

/* ==========================================================================
   Public API
   ========================================================================== */

function bindRoots() {
  if (state.bound) return;
  state.bound = true;
  const open = (event) => {
    const card = event.target.closest('[data-property-id]');
    if (card) openPropertyDetail(card.dataset.propertyId);
  };
  roots.catalog().addEventListener('click', (event) => {
    if (event.target.closest('[data-action="clear-filters"]')) {
      document.getElementById('catalog-filters').reset();
      document.getElementById('filter-query').focus();
      return;
    }
    open(event);
  });
  roots.inquiries().addEventListener('click', open);
}

export function mountClientUI(user) {
  state.user = user;
  state.filters = { ...EMPTY_FILTERS };
  roots.catalog().innerHTML = `<div class="section-stack">${filterBarMarkup()}<div id="catalog-results"></div></div>`;
  bindFilters(document.getElementById('catalog-filters'));
  bindRoots();
  renderCatalogResults({ intro: true });
  renderInquiries();
}

/** Re-renders only what depends on the changed entity. */
export function refreshClientUI(entity) {
  if (!state.user) return;
  if (['properties', 'users', 'all'].includes(entity)) renderCatalogResults();
  if (['leads', 'properties', 'users', 'all'].includes(entity)) renderInquiries();
  updateDetail();
}

export function unmountClientUI() {
  state.user = null;
  state.detail = null;
  roots.catalog().innerHTML = '';
  roots.inquiries().innerHTML = '';
  updateInquiryCount(0);
}
