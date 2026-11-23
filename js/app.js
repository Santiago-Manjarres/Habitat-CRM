/**
 * Habitat CRM — entry point of the interface.
 * Guards the views by session, handles hash navigation, asks the Java server for
 * the data and redraws when it changes (in this tab or in another one).
 */
import { APP_NAME, PLACEHOLDER_IMAGE } from './config.js';
import { setSessionExpiredHandler } from './services/api.js';
import { loadState, clearState, getVersion } from './services/dataService.js';
import { initSync, subscribe } from './services/syncService.js';
import { getActiveSession, revalidateSession, isStaff, logout } from './services/authService.js';
import { showToast, closeAllModals } from './ui/toastUI.js';
import { initAuthUI, renderUserChips, resetLoginScreen } from './ui/authUI.js';
import { mountAdminUI, refreshAdminUI, unmountAdminUI, renderAdminLoading } from './ui/adminUI.js';

const VIEWS = ['login', 'admin'];

const ROUTES = {
  admin: { resumen: 'Resumen', propiedades: 'Propiedades', clientes: 'Clientes' },
};

const CONTROLLERS = {
  admin: { mount: mountAdminUI, refresh: refreshAdminUI, unmount: unmountAdminUI, loading: renderAdminLoading },
};

let currentView = null;
let mounted = null; // { view, userId }

/* ---------- Views ---------- */

function viewForUser(user) {
  return user && isStaff(user) ? 'admin' : 'login';
}

function showView(name) {
  VIEWS.forEach((view) => { document.getElementById(`view-${view}`).hidden = view !== name; });
  document.body.dataset.view = name;
  currentView = name;
  if (name === 'login') document.title = `Iniciar sesión · ${APP_NAME}`;
}

function mountView(view, user) {
  if (mounted?.view === view && mounted.userId === user.id) return;
  unmountViews();
  CONTROLLERS[view].mount(user);
  mounted = { view, userId: user.id };
}

function unmountViews() {
  if (mounted) CONTROLLERS[mounted.view].unmount();
  mounted = null;
}

/* ---------- Sections (hash navigation inside a view) ---------- */

function currentRoute(view) {
  const routes = Object.keys(ROUTES[view]);
  const requested = window.location.hash.replace(/^#\/?/, '');
  return routes.includes(requested) ? requested : routes[0];
}

function applySection(view, { moveFocus = false } = {}) {
  const route = currentRoute(view);
  const root = document.getElementById(`view-${view}`);
  root.querySelectorAll('[data-section]').forEach((section) => { section.hidden = section.dataset.section !== route; });
  root.querySelectorAll('[data-route]').forEach((link) => {
    if (link.dataset.route === route) link.setAttribute('aria-current', 'page');
    else link.removeAttribute('aria-current');
  });
  document.title = `${ROUTES[view][route]} · ${APP_NAME}`;
  if (moveFocus) root.querySelector(`[data-section="${route}"] .page__title`)?.focus();
}

function handleHashChange() {
  if (currentView === 'admin') applySection(currentView, { moveFocus: true });
}

/* ---------- Session guard ---------- */

/** Renders exactly one view according to the active session and its role. */
async function route({ moveFocus = false } = {}) {
  const session = getActiveSession();
  if (!session) {
    showView('login');
    return;
  }
  const expectedView = viewForUser(session);
  if (!mounted && expectedView !== 'login') {
    showView(expectedView);
    CONTROLLERS[expectedView].loading();
  }
  if (!getVersion() && !(await loadState())) {
    if (getActiveSession()) showToast('No se pudo conectar con el servidor. Revisa que Habitat CRM esté encendido', { type: 'error' });
    return;
  }
  const user = revalidateSession();
  if (!user) {
    unmountViews();
    showView('login');
    return;
  }
  const view = viewForUser(user);
  renderUserChips(user);
  showView(view);
  if (view === 'login') return;
  mountView(view, user);
  applySection(view, { moveFocus });
}

function handleLogin(user) {
  showToast(`Hola, ${user.name.split(' ')[0]}`, { type: 'success' });
  history.replaceState(null, '', `#/${Object.keys(ROUTES[viewForUser(user)])[0]}`);
  route({ moveFocus: true });
}

function handleLogout({ message = 'Cerraste sesión', type = 'info' } = {}) {
  closeAllModals();
  unmountViews();
  clearState();
  history.replaceState(null, '', window.location.pathname + window.location.search);
  renderUserChips(null);
  showView('login');
  resetLoginScreen();
  showToast(message, { type });
}

/* ---------- Data changes (this tab or others) ---------- */

function handleDataChange(event) {
  if (!mounted) return;
  if (event.entity === 'users' || event.entity === 'all') {
    const user = revalidateSession();
    if (!user) {
      handleLogout({ message: 'Tu cuenta ya no existe en los datos actuales. Inicia sesión de nuevo', type: 'warning' });
      return;
    }
    renderUserChips(user);
  }
  CONTROLLERS[mounted.view].refresh(event.entity);
}

/** Broken or unreachable images fall back to the local SVG placeholder. */
function handleImageError(event) {
  const image = event.target;
  if (!(image instanceof HTMLImageElement) || !image.hasAttribute('data-fallback')) return;
  if (image.dataset.fallback === 'applied') return;
  image.dataset.fallback = 'applied';
  image.src = PLACEHOLDER_IMAGE;
}

/* ---------- Boot ---------- */

/** The server forgot this session (it was restarted, for example): back to the login. */
function handleSessionExpired() {
  if (!getActiveSession()) return;
  logout();
  handleLogout({ message: 'Tu sesión terminó. Inicia sesión de nuevo', type: 'warning' });
}

function boot() {
  document.getElementById('boot-notice')?.remove();
  document.addEventListener('error', handleImageError, true);
  setSessionExpiredHandler(handleSessionExpired);
  initSync({ reload: loadState, version: getVersion });
  initAuthUI({ onLogin: handleLogin, onLogout: handleLogout });
  subscribe(handleDataChange);
  window.addEventListener('hashchange', handleHashChange);
  route();
}

boot();
