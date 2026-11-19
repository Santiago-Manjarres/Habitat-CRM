/**
 * Habitat CRM — entry point.
 * Boots storage and sync, guards views by session/role, handles hash navigation
 * inside each view, mounts the role UI and forwards data changes (from this or
 * other tabs) so only the affected blocks re-render.
 */
import { APP_NAME, PLACEHOLDER_IMAGE } from './config.js';
import { initStorage } from './services/storageService.js';
import { initSync, subscribe } from './services/syncService.js';
import { getActiveSession, revalidateSession, isStaff } from './services/authService.js';
import { showToast, closeAllModals } from './ui/toastUI.js';
import { initAuthUI, renderUserChips, resetLoginScreen } from './ui/authUI.js';
import { mountClientUI, refreshClientUI, unmountClientUI, renderClientLoading } from './ui/clientUI.js';
import { mountAdminUI, refreshAdminUI, unmountAdminUI, renderAdminLoading } from './ui/adminUI.js';

const VIEWS = ['login', 'client', 'admin'];

const ROUTES = {
  client: { catalogo: 'Catálogo', consultas: 'Mis consultas' },
  admin: { resumen: 'Resumen', propiedades: 'Propiedades', leads: 'Leads', datos: 'Datos' },
};

const CONTROLLERS = {
  client: { mount: mountClientUI, refresh: refreshClientUI, unmount: unmountClientUI, loading: renderClientLoading },
  admin: { mount: mountAdminUI, refresh: refreshAdminUI, unmount: unmountAdminUI, loading: renderAdminLoading },
};

let dataReady = Promise.resolve();
let currentView = null;
let mounted = null; // { view, userId }

/* ---------- Views ---------- */

function viewForUser(user) {
  if (!user) return 'login';
  return isStaff(user) ? 'admin' : 'client';
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
  if (currentView === 'client' || currentView === 'admin') applySection(currentView, { moveFocus: true });
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
  if (!mounted) {
    showView(expectedView);
    CONTROLLERS[expectedView].loading();
  }
  await dataReady;
  const user = revalidateSession();
  if (!user) {
    unmountViews();
    showView('login');
    return;
  }
  const view = viewForUser(user);
  renderUserChips(user);
  showView(view);
  mountView(view, user);
  applySection(view, { moveFocus });
}

function handleLogin(user, { isNew = false } = {}) {
  const firstName = user.name.split(' ')[0];
  showToast(isNew ? `Cuenta creada. Te damos la bienvenida, ${firstName}` : `Hola, ${firstName}`, { type: 'success' });
  history.replaceState(null, '', `#/${Object.keys(ROUTES[viewForUser(user)])[0]}`);
  route({ moveFocus: true });
}

function handleLogout({ message = 'Cerraste sesión', type = 'info' } = {}) {
  closeAllModals();
  unmountViews();
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

function loadData() {
  return initStorage().catch((error) => {
    console.error('[app] Storage initialization failed', error);
    showToast(error.message || 'No se pudieron cargar los datos iniciales', { type: 'error' });
  });
}

function boot() {
  document.getElementById('boot-notice')?.remove();
  document.addEventListener('error', handleImageError, true);
  initSync();
  dataReady = loadData();
  initAuthUI({ onLogin: handleLogin, onLogout: handleLogout, whenReady: dataReady });
  subscribe(handleDataChange);
  window.addEventListener('hashchange', handleHashChange);
  route();
}

boot();
