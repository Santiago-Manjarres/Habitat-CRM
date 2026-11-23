/**
 * Keeps every open tab up to date.
 *  - Same browser: a BroadcastChannel message arrives as soon as a tab saves.
 *  - Other browsers or computers: every 2 seconds the tab asks the Java server
 *    for its data version (GET /api/version); if it changed, it reloads.
 */
import { SYNC_CHANNEL_NAME, SYNC_POLL_MS } from '../config.js';
import { request } from './api.js';

const listeners = new Set();
let channel = null;
let reload = async () => false;
let knownVersion = () => 0;

function emit(entity, origin) {
  listeners.forEach((listener) => {
    try {
      listener({ type: 'DATA_UPDATED', entity, origin });
    } catch (error) {
      console.error('[syncService] Listener failed', error);
    }
  });
}

async function reloadAndEmit() {
  if (await reload()) emit('all', 'remote');
}

async function checkVersion() {
  if (document.hidden) return;
  const data = await request('GET', '/version');
  if (data.ok && knownVersion() && data.version !== knownVersion()) reloadAndEmit();
}

/** options.reload() downloads the data again; options.version() says which version this tab has. */
export function initSync(options) {
  reload = options.reload;
  knownVersion = options.version;
  if (typeof BroadcastChannel !== 'undefined') {
    channel = new BroadcastChannel(SYNC_CHANNEL_NAME);
    channel.onmessage = () => reloadAndEmit();
  }
  setInterval(checkVersion, SYNC_POLL_MS);
}

/** Called after this tab saved something: tell the other tabs and redraw this one. */
export function notifyChange(entity) {
  channel?.postMessage({ type: 'DATA_UPDATED', entity });
  emit(entity, 'local');
}

export function subscribe(listener) {
  listeners.add(listener);
  return () => listeners.delete(listener);
}
