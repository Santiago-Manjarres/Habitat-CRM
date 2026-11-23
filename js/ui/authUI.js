/**
 * Login screen: show-hide password, demo autofill buttons, logout buttons and the
 * user chip. The username and password are checked by the Java server. Also exports the shared form helpers (field errors, live validation,
 * loading buttons) reused by the advisor panel.
 */
import { ROLE_LABELS } from '../config.js';
import { escapeHTML, iconMarkup } from '../utils/formatters.js';
import { login, logout } from '../services/authService.js';

const DEMO_CREDENTIALS = {
  admin: { username: 'admin', password: 'admin' },
  andres: { username: 'andres', password: '1234' },
};

let callbacks = { onLogin: () => {}, onLogout: () => {} };

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

/**
 * After the first failed submit, check the form again while the advisor types.
 * validate() asks the Java server and resolves to { field: message }; it runs
 * 250 ms after the last key so the server is not called on every letter.
 */
export function enableLiveValidation(form, validate) {
  if (form.dataset.live === 'true') return;
  form.dataset.live = 'true';
  let timer = null;
  form.addEventListener('input', () => {
    clearTimeout(timer);
    timer = setTimeout(async () => {
      const errors = await validate();
      if (form.isConnected) showFieldErrors(form, errors);
    }, 250);
  });
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
  clearFieldErrors(form);
  setButtonLoading(submit, true);
  const result = await login(values.username, values.password);
  setButtonLoading(submit, false);
  if (result.fieldErrors) {
    showFieldErrors(form, result.fieldErrors)?.focus();
    return;
  }
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
   Public API
   ========================================================================== */

/** Binds every login-screen and logout control once. */
export function initAuthUI({ onLogin, onLogout }) {
  callbacks = { onLogin, onLogout };
  bindPasswordToggle();
  bindDemoButtons();
  const loginForm = document.getElementById('login-form');
  loginForm.addEventListener('submit', handleLoginSubmit);
  loginForm.addEventListener('input', (event) => { if (event.target.value) setFieldError(event.target, ''); });
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
  form.elements.username.focus();
}
