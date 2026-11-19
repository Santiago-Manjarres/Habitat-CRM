/**
 * Formatting helpers: currency, numbers, dates, relative time and HTML escaping.
 * Pure functions — no DOM, no storage.
 */
import { LOCALE, DEFAULT_CURRENCY, PLACEHOLDER_IMAGE } from '../config.js';

const HTML_ESCAPES = {
  '&': '&amp;',
  '<': '&lt;',
  '>': '&gt;',
  '"': '&quot;',
  "'": '&#39;',
  '`': '&#96;',
};

/** Escapes a value so it can be safely interpolated into innerHTML. */
export function escapeHTML(value) {
  if (value === null || value === undefined) return '';
  return String(value).replace(/[&<>"'`]/g, (char) => HTML_ESCAPES[char]);
}

const currencyFormatters = new Map();

function getCurrencyFormatter(currency) {
  if (!currencyFormatters.has(currency)) {
    currencyFormatters.set(currency, new Intl.NumberFormat(LOCALE, {
      style: 'currency',
      currency,
      maximumFractionDigits: 0,
    }));
  }
  return currencyFormatters.get(currency);
}

/** 450000000 -> "$ 450.000.000" */
export function formatCurrency(value, currency = DEFAULT_CURRENCY) {
  const amount = Number(value);
  if (!Number.isFinite(amount)) return '—';
  try {
    return getCurrencyFormatter(currency).format(amount);
  } catch {
    return getCurrencyFormatter(DEFAULT_CURRENCY).format(amount);
  }
}

/** Compact currency for KPIs: 4870000000 -> "$ 4.870 M" */
export function formatCompactCurrency(value) {
  const amount = Number(value);
  if (!Number.isFinite(amount)) return '—';
  if (Math.abs(amount) < 1_000_000) return formatCurrency(amount);
  return `$ ${formatNumber(Math.round(amount / 1_000_000))} M`;
}

/** 1234567 -> "1.234.567" */
export function formatNumber(value) {
  const number = Number(value);
  if (!Number.isFinite(number)) return '—';
  return new Intl.NumberFormat(LOCALE, { maximumFractionDigits: 0 }).format(number);
}

/** Parses user-typed numbers with thousand separators: "$ 1.200.000" -> 1200000 */
export function parseNumber(input) {
  if (typeof input === 'number') return input;
  const cleaned = String(input ?? '').trim().replace(/[^\d,-]/g, '').replace(',', '.');
  if (cleaned === '' || cleaned === '-') return NaN;
  return Number(cleaned);
}

/** 115 -> "115 m²" */
export function formatArea(value) {
  return `${formatNumber(value)} m²`;
}

/** ISO -> "15 nov 2026" */
export function formatDate(iso) {
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return '—';
  return new Intl.DateTimeFormat(LOCALE, { day: 'numeric', month: 'short', year: 'numeric' }).format(date);
}

const RELATIVE_UNITS = [
  ['year', 31_536_000],
  ['month', 2_592_000],
  ['week', 604_800],
  ['day', 86_400],
  ['hour', 3_600],
  ['minute', 60],
];

/** ISO -> "hace 5 minutos", "ayer", "justo ahora" */
export function formatRelativeTime(iso, now = Date.now()) {
  const time = new Date(iso).getTime();
  if (Number.isNaN(time)) return '—';
  const seconds = Math.round((time - now) / 1000);
  if (Math.abs(seconds) < 45) return 'justo ahora';
  const formatter = new Intl.RelativeTimeFormat('es', { numeric: 'auto' });
  const [unit, size] = RELATIVE_UNITS.find(([, s]) => Math.abs(seconds) >= s) ?? RELATIVE_UNITS.at(-1);
  return formatter.format(Math.round(seconds / size), unit);
}

/** Lowercase without accents, for search: "Bogotá" -> "bogota" */
export function normalizeText(value) {
  return String(value ?? '')
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '')
    .toLowerCase()
    .trim();
}

/** (12, "propiedad", "propiedades") -> "12 propiedades" */
export function pluralize(count, singular, plural) {
  return `${formatNumber(count)} ${count === 1 ? singular : plural}`;
}

/** Markup for a decorative sprite icon: iconMarkup('search', 'icon--sm'). */
export function iconMarkup(name, extraClass = '') {
  const safeName = String(name).replace(/[^a-z-]/g, '');
  const classes = `icon ${escapeHTML(extraClass)}`.trim();
  return `<svg class="${classes}" aria-hidden="true" focusable="false"><use href="#i-${safeName}"></use></svg>`;
}

/** Status pill: text is always present, color only reinforces it. */
export function badgeMarkup(label, tone = 'neutral', { plain = false } = {}) {
  const toneClass = `badge--${String(tone).replace(/[^a-z]/g, '')}`;
  return `<span class="badge ${toneClass}${plain ? ' badge--plain' : ''}">${escapeHTML(label)}</span>`;
}

/**
 * <img> with explicit dimensions (no layout shift), lazy loading and the local
 * placeholder when there is no URL. Broken URLs are swapped by a global
 * 'error' listener in app.js (data-fallback).
 */
export function imageMarkup(url, { alt = '', className = '', width = 800, height = 600, lazy = true } = {}) {
  const src = String(url ?? '').trim() || PLACEHOLDER_IMAGE;
  const loading = lazy ? ' loading="lazy"' : '';
  return `<img src="${escapeHTML(src)}" alt="${escapeHTML(alt)}" width="${width}" height="${height}" class="${escapeHTML(className)}" decoding="async"${loading} data-fallback>`;
}

/** "Laura Mendoza" -> "LM" */
export function getInitials(name) {
  const words = String(name ?? '').trim().split(/\s+/).filter(Boolean);
  const initials = words.slice(0, 2).map((word) => word[0].toUpperCase()).join('');
  return initials || '?';
}
