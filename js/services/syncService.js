/**
 * Cross-tab synchronization.
 * Primary transport: BroadcastChannel ('inmocrm_sync_channel'): one message per save,
 * delivered and emitted immediately (no timers, so hidden tabs are not throttled).
 * Fallback: the window 'storage' event, used only where BroadcastChannel is unavailable,
 * so a change never produces two renders.
 */
import { SYNC_CHANNEL_NAME, STORAGE_KEYS } from '../config.js';

const listeners = new Set();
const tabId = createTabId();
let channel = null;
let started = false;

function createTabId() {
  try {
    return crypto.randomUUID();
  } catch {
    return `${Date.now().toString(36)}${Math.random().toString(36).slice(2)}`;
  }
}

function emit(event) {
  listeners.forEach((listener) => {
    try {
      listener(event);
    } catch (error) {
      console.error('[syncService] Listener failed', error);
    }
  });
}

function emitRemote(entity) {
  emit({ type: 'DATA_UPDATED', entity, origin: 'remote', timestamp: Date.now() });
}

function handleChannelMessage({ data }) {
  if (!data || data.type !== 'DATA_UPDATED' || data.sourceId === tabId) return;
  emitRemote(data.entity);
}

function handleStorageEvent(event) {
  if (channel) return; // BroadcastChannel already delivers this change
  if (event.key === null) {
    emitRemote('all');
    return;
  }
  const entity = Object.keys(STORAGE_KEYS).find((name) => STORAGE_KEYS[name] === event.key);
  if (entity) emitRemote(entity);
}

function openChannel() {
  if (typeof BroadcastChannel === 'undefined') return null;
  try {
    const instance = new BroadcastChannel(SYNC_CHANNEL_NAME);
    instance.onmessage = handleChannelMessage;
    return instance;
  } catch (error) {
    console.warn('[syncService] BroadcastChannel unavailable, using storage events only', error);
    return null;
  }
}

/** Starts listening to other tabs. Safe to call more than once. */
export function initSync() {
  if (started) return;
  started = true;
  channel = openChannel();
  window.addEventListener('storage', handleStorageEvent);
}

/**
 * Announces that an entity changed ('users' | 'properties' | 'leads' | 'all').
 * Other tabs receive it through the channel; this tab's listeners are notified
 * immediately with origin 'local'.
 */
export function notifyChange(entity) {
  const message = { type: 'DATA_UPDATED', entity, timestamp: Date.now(), sourceId: tabId };
  try {
    channel?.postMessage(message);
  } catch (error) {
    console.warn('[syncService] Could not broadcast change', error);
  }
  emit({ type: message.type, entity, origin: 'local', timestamp: message.timestamp });
}

/** Subscribes to data changes. Returns an unsubscribe function. */
export function subscribe(listener) {
  listeners.add(listener);
  return () => listeners.delete(listener);
}

export function isRealtimeSupported() {
  return channel !== null;
}
