/**
 * Visual feedback layer.
 * - Toasts: non-blocking (bottom-right, centered on mobile). Max 3 visible; 4s (errors 6s);
 *   pause on hover/focus and while the tab is hidden; dismissible.
 * - Dialogs: native <dialog> + showModal() (focus trap, Esc, backdrop) used for detail,
 *   forms and confirmations, replacing alert()/confirm(). Focus returns to the opener.
 */
import { TOAST_DURATION_MS, TOAST_ERROR_DURATION_MS, TOAST_MAX_VISIBLE } from '../config.js';
import { iconMarkup, escapeHTML } from '../utils/formatters.js';

const ICONS = { success: 'check-circle', error: 'alert-circle', warning: 'alert-triangle', info: 'info' };
const REMOVE_FALLBACK_MS = 400;
const active = new Set();

function getRegion() {
  return document.getElementById('toast-region');
}

function createToastElement(message, type) {
  const toast = document.createElement('div');
  toast.className = `toast toast--${type}`;
  toast.dataset.state = 'entering';
  toast.dataset.motion = 'fade';
  if (type === 'error') toast.setAttribute('role', 'alert');
  toast.innerHTML = `
    <span class="toast__icon">${iconMarkup(ICONS[type] ?? ICONS.info)}</span>
    <p class="toast__message"></p>
    <button type="button" class="btn btn--ghost btn--icon btn--sm toast__close" aria-label="Cerrar notificación">
      ${iconMarkup('close', 'icon--sm')}
    </button>`;
  toast.querySelector('.toast__message').textContent = message;
  return toast;
}

/** Timer that can be paused (hover, focus, hidden tab) and resumed. */
function createTimer(duration, onDone) {
  let remaining = duration;
  let startedAt = 0;
  let handle = null;
  const pause = () => {
    if (!handle) return;
    clearTimeout(handle);
    handle = null;
    remaining -= Date.now() - startedAt;
  };
  const resume = () => {
    if (handle || remaining <= 0) return;
    startedAt = Date.now();
    handle = setTimeout(onDone, remaining);
  };
  return { pause, resume, clear: () => clearTimeout(handle) };
}

function bindPauseEvents(toast, timer) {
  toast.addEventListener('mouseenter', timer.pause);
  toast.addEventListener('mouseleave', timer.resume);
  toast.addEventListener('focusin', timer.pause);
  toast.addEventListener('focusout', timer.resume);
}

function dismiss(entry) {
  if (!active.has(entry)) return;
  active.delete(entry);
  entry.timer.clear();
  const { element } = entry;
  element.dataset.state = 'leaving';
  const remove = () => element.remove();
  element.addEventListener('transitionend', remove, { once: true });
  setTimeout(remove, REMOVE_FALLBACK_MS);
}

function enforceLimit() {
  const entries = [...active];
  while (entries.length >= TOAST_MAX_VISIBLE) dismiss(entries.shift());
}

/**
 * Shows a toast. type: 'success' | 'error' | 'warning' | 'info'.
 * Returns a function that dismisses it.
 */
export function showToast(message, { type = 'info', duration } = {}) {
  const region = getRegion();
  if (!region) return () => {};
  enforceLimit();
  const element = createToastElement(String(message), type);
  const entry = { element, timer: null };
  entry.timer = createTimer(duration ?? (type === 'error' ? TOAST_ERROR_DURATION_MS : TOAST_DURATION_MS), () => dismiss(entry));
  bindPauseEvents(element, entry.timer);
  element.querySelector('.toast__close').addEventListener('click', () => dismiss(entry));
  region.append(element);
  active.add(entry);
  requestAnimationFrame(() => requestAnimationFrame(() => { element.dataset.state = 'open'; }));
  if (!document.hidden) entry.timer.resume();
  return () => dismiss(entry);
}

document.addEventListener('visibilitychange', () => {
  active.forEach((entry) => (document.hidden ? entry.timer.pause() : entry.timer.resume()));
});

/* ==========================================================================
   Dialogs
   ========================================================================== */

let dialogCounter = 0;

function buildDialog({ title, body, footer, size }) {
  const dialog = document.createElement('dialog');
  const titleId = `dialog-title-${++dialogCounter}`;
  dialog.className = `modal${size === 'sm' ? ' modal--sm' : ''}`;
  dialog.dataset.motion = 'fade';
  dialog.setAttribute('aria-labelledby', titleId);
  dialog.innerHTML = `
    <div class="modal__inner">
      <header class="modal__header">
        <h2 class="modal__title" id="${titleId}"></h2>
        <button type="button" class="btn btn--ghost btn--icon" data-close aria-label="Cerrar">${iconMarkup('close')}</button>
      </header>
      <div class="modal__body">${body}</div>
      ${footer ? `<footer class="modal__footer">${footer}</footer>` : ''}
    </div>`;
  dialog.querySelector('.modal__title').textContent = title;
  return dialog;
}

function bindDialogClose(dialog, close) {
  dialog.addEventListener('cancel', (event) => {
    event.preventDefault();
    close();
  });
  dialog.addEventListener('click', (event) => {
    if (event.target === dialog || event.target.closest('[data-close]')) close();
  });
}

/**
 * Opens a modal dialog. `body` and `footer` are HTML strings that the caller has
 * already escaped. Returns { element, close }. onClose(value) runs once.
 */
export function openModal({ title, body, footer = '', size = 'md', onClose = () => {} }) {
  const opener = document.activeElement;
  const dialog = buildDialog({ title, body, footer, size });
  let closed = false;
  const finish = (value) => {
    if (dialog.open) dialog.close();
    dialog.remove();
    if (opener?.isConnected) opener.focus();
    onClose(value);
  };
  const close = (value) => {
    if (closed) return;
    closed = true;
    dialog.dataset.state = 'closing';
    const onEnd = (event) => {
      if (event.target !== dialog) return;
      dialog.removeEventListener('transitionend', onEnd);
      if (dialog.isConnected) finish(value);
    };
    dialog.addEventListener('transitionend', onEnd);
    setTimeout(() => { if (dialog.isConnected) finish(value); }, REMOVE_FALLBACK_MS);
  };
  bindDialogClose(dialog, close);
  document.body.append(dialog);
  dialog.showModal();
  return { element: dialog, close };
}

/**
 * Confirmation dialog (replaces confirm()). Initial focus on "Cancelar";
 * the destructive action sits on the right. Resolves true/false.
 * messageHTML must be escaped by the caller.
 */
export function confirmDialog({ title, messageHTML, confirmLabel, tone = 'danger' }) {
  return new Promise((resolve) => {
    const footer = `
      <button type="button" class="btn btn--secondary" data-close autofocus>Cancelar</button>
      <button type="button" class="btn btn--${tone === 'danger' ? 'danger' : 'primary'}" data-confirm>${escapeHTML(confirmLabel)}</button>`;
    const modal = openModal({ title, body: `<p>${messageHTML}</p>`, footer, size: 'sm', onClose: (value) => resolve(value === true) });
    modal.element.querySelector('[data-confirm]').addEventListener('click', () => modal.close(true));
  });
}

/** Closes every open dialog (used on logout). */
export function closeAllModals() {
  document.querySelectorAll('dialog.modal[open]').forEach((dialog) => dialog.dispatchEvent(new Event('cancel', { cancelable: true })));
}
