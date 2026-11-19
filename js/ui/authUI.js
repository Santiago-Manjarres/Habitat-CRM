/**
 * Login screen: login / register tabs, show-hide password, demo autofill buttons,
 * logout buttons and the user chip. Also exports the shared form helpers
 * (field errors, live validation, loading buttons) reused by the other views.
 */
import { ROLE_LABELS } from '../config.js';
import { escapeHTML, iconMarkup } from '../utils/formatters.js';
import { validateLoginForm, validateRegistrationForm } from '../utils/validators.js';
import { login, registerClient, logout } from '../services/authService.js';

const DEMO_CREDENTIALS = {
  admin: { username: 'admin', password: 'admin' },
  client: { username: 'client', password: 'client' },
};

let callbacks = { onLogin: () => {}, onLogout: () => {}, whenReady: Promise.resolve() };

/* ==========================================================================
   Shared form helpers
   ========================================================================== */

/** Marks a field as invalid (aria-invalid + message linked via aria-describedby). */
export function setFieldError(input, message) {
  const errorElement = document.getElementById(`${input.id}-error`);
  if (message) input.setAttribute('aria-invalid', 'true');
  else input.removeAttribute('aria-invalid');
  if (!errorElement) return;
  errorElement.innerHTML = message ? `${iconMarkup('alert-circle', 'icon--sm')}<span>${escapeHTML(message)}</span>` : '';
}

/** Applies { field: message } to a form; returns the first invalid input. */
export function showFieldErrors(form, errors) {
  let firstInvalid = null;
  [...form.elements].forEach((element) => {
    if (!element.name || element.type === 'button' || element.type === 'submit') return;
    const message = errors[element.name] ?? '';
    setFieldError(element, message);
    if (message && !firstInvalid) firstInvalid = element;
  });
  return firstInvalid;
}

export function clearFieldErrors(form) {
  showFieldErrors(form, {});
}

/** After the first failed submit, re-validate on every input. */
export function enableLiveValidation(form, validate) {
  if (form.dataset.live === 'true') return;
  form.dataset.live = 'true';
  form.addEventListener('input', () => showFieldErrors(form, validate()));
}

export function setButtonLoading(button, isLoading) {
  if (!button) return;
  button.classList.toggle('is-loading', isLoading);
  button.disabled = isLoading;
  button.setAttribute('aria-busy', String(isLoading));
}

function setFormAlert(id, message) {
  const alert = document.getElementById(id);
  if (!alert) return;
  alert.innerHTML = message ? `${iconMarkup('alert-circle', 'icon--sm')}<span>${escapeHTML(message)}</span>` : '';
}

function readForm(form) {
  return Object.fromEntries(new FormData(form).entries());
}

/* ==========================================================================
   Tabs (roving tabindex, arrow keys, no animation)
   ========================================================================== */

function selectTab(tab, { focus = false } = {}) {
  const tabs = [...tab.closest('[role="tablist"]').querySelectorAll('[role="tab"]')];
  tabs.forEach((item) => {
    const selected = item === tab;
    item.setAttribute('aria-selected', String(selected));
    item.tabIndex = selected ? 0 : -1;
    document.getElementById(item.getAttribute('aria-controls')).hidden = !selected;
  });
  if (focus) tab.focus();
}

function handleTabKeydown(event) {
  const tabs = [...event.currentTarget.querySelectorAll('[role="tab"]')];
  const index = tabs.indexOf(document.activeElement);
  const moves = { ArrowRight: 1, ArrowLeft: -1, Home: -index, End: tabs.length - 1 - index };
  if (index === -1 || !(event.key in moves)) return;
  event.preventDefault();
  selectTab(tabs[(index + moves[event.key] + tabs.length) % tabs.length], { focus: true });
}

function bindTabs() {
  const tablist = document.querySelector('.auth-card [role="tablist"]');
  tablist.addEventListener('click', (event) => {
    const tab = event.target.closest('[role="tab"]');
    if (tab) selectTab(tab);
  });
  tablist.addEventListener('keydown', handleTabKeydown);
}

/* ==========================================================================
   Login form
   ========================================================================== */

function setPasswordVisibility(show) {
  const button = document.getElementById('toggle-password');
  document.getElementById('login-password').type = show ? 'text' : 'password';
  button.setAttribute('aria-pressed', String(show));
  button.setAttribute('aria-label', show ? 'Ocultar contraseña' : 'Mostrar contraseña');
  button.querySelector('use').setAttribute('href', show ? '#i-eye-off' : '#i-eye');
}

function bindPasswordToggle() {
  document.getElementById('toggle-password').addEventListener('click', () => {
    setPasswordVisibility(document.getElementById('login-password').type === 'password');
  });
}

function bindDemoButtons() {
  document.querySelectorAll('[data-demo-user]').forEach((button) => {
    button.addEventListener('click', () => {
      const form = document.getElementById('login-form');
      const { username, password } = DEMO_CREDENTIALS[button.dataset.demoUser];
      form.elements.username.value = username;
      form.elements.password.value = password;
      clearFieldErrors(form);
      setFormAlert('login-error', '');
      form.querySelector('[type="submit"]').focus();
    });
  });
}

async function handleLoginSubmit(event) {
  event.preventDefault();
  const form = event.currentTarget;
  const submit = form.querySelector('[type="submit"]');
  const values = readForm(form);
  setFormAlert('login-error', '');
  const check = validateLoginForm(values);
  if (!check.valid) {
    showFieldErrors(form, check.errors)?.focus();
    enableLiveValidation(form, () => validateLoginForm(readForm(form)).errors);
    return;
  }
  clearFieldErrors(form);
  setButtonLoading(submit, true);
  await callbacks.whenReady;
  const result = login(values.username, values.password);
  setButtonLoading(submit, false);
  if (!result.ok) {
    setFormAlert('login-error', result.error);
    form.elements.password.select();
    return;
  }
  form.reset();
  setPasswordVisibility(false);
  callbacks.onLogin(result.user);
}

/* ==========================================================================
   Register form
   ========================================================================== */

async function handleRegisterSubmit(event) {
  event.preventDefault();
  const form = event.currentTarget;
  const submit = form.querySelector('[type="submit"]');
  setFormAlert('register-error', '');
  setButtonLoading(submit, true);
  await callbacks.whenReady;
  const result = registerClient(readForm(form));
  setButtonLoading(submit, false);
  if (!result.ok) {
    showFieldErrors(form, result.fieldErrors)?.focus();
    enableLiveValidation(form, () => validateRegistrationForm(readForm(form)).errors);
    return;
  }
  form.reset();
  clearFieldErrors(form);
  selectTab(document.getElementById('tab-login'));
  callbacks.onLogin(result.user, { isNew: true });
}

/* ==========================================================================
   Public API
   ========================================================================== */

/** Binds every login-screen and logout control once. */
export function initAuthUI({ onLogin, onLogout, whenReady }) {
  callbacks = { onLogin, onLogout, whenReady: whenReady ?? Promise.resolve() };
  bindTabs();
  bindPasswordToggle();
  bindDemoButtons();
  document.getElementById('login-form').addEventListener('submit', handleLoginSubmit);
  document.getElementById('register-form').addEventListener('submit', handleRegisterSubmit);
  document.addEventListener('click', (event) => {
    if (!event.target.closest('[data-action="logout"]')) return;
    logout();
    callbacks.onLogout();
  });
}

/** Fills every user chip (navbar / sidebar) with the session user. */
export function renderUserChips(user) {
  document.querySelectorAll('[data-bind="user-name"]').forEach((el) => { el.textContent = user?.name ?? ''; });
  document.querySelectorAll('[data-bind="user-role"]').forEach((el) => { el.textContent = ROLE_LABELS[user?.role] ?? ''; });
  document.querySelectorAll('[data-bind="user-avatar"]').forEach((el) => { el.textContent = user?.avatar ?? ''; });
}

/** Resets the login screen to a clean state and focuses the username field. */
export function resetLoginScreen() {
  const form = document.getElementById('login-form');
  form.reset();
  clearFieldErrors(form);
  setFormAlert('login-error', '');
  setPasswordVisibility(false);
  selectTab(document.getElementById('tab-login'));
  form.elements.username.focus();
}
