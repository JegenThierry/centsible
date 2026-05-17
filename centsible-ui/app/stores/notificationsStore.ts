import {defineStore} from 'pinia';
import {useNotificationService} from "~/services/notifications/notification-service";
import type {Notification} from "~/models/notification/notification";

const POLL_INTERVAL_MS = 60_000;

export const useNotificationsStore = defineStore('notificationsStore', () => {
  const service = useNotificationService(useApi());
  const notifications = ref<Notification[]>([]);
  const unreadCount = ref(0);
  const loading = ref(false);

  let pollTimer: ReturnType<typeof setInterval> | null = null;

  function fingerprint(xs: Notification[]): string {
    return xs.map((n) => `${n.id}|${n.readAt ?? ''}`).join(',');
  }

  async function refresh() {
    loading.value = true;
    try {
      const next = await service.list(50);
      if (fingerprint(next) !== fingerprint(notifications.value)) {
        notifications.value = next;
      }
      unreadCount.value = next.filter((n) => !n.readAt).length;
    } catch (e) {
      console.error('Failed to load notifications', e);
    } finally {
      loading.value = false;
    }
  }

  async function refreshCount() {
    try {
      unreadCount.value = await service.unreadCount();
    } catch (e) {
      // silent
    }
  }

  async function markRead(id: string) {
    await service.markRead(id);
    const n = notifications.value.find((x) => x.id === id);
    if (n && !n.readAt) {
      n.readAt = new Date().toISOString();
      unreadCount.value = Math.max(0, unreadCount.value - 1);
    }
  }

  async function markAllRead() {
    await service.markAllRead();
    const now = new Date().toISOString();
    notifications.value.forEach((n) => {
      if (!n.readAt) n.readAt = now;
    });
    unreadCount.value = 0;
  }

  async function remove(id: string) {
    await service.remove(id);
    const idx = notifications.value.findIndex((n) => n.id === id);
    if (idx >= 0) {
      const [removed] = notifications.value.splice(idx, 1);
      if (removed && !removed.readAt) unreadCount.value = Math.max(0, unreadCount.value - 1);
    }
  }

  function startPolling() {
    if (pollTimer) return;
    pollTimer = setInterval(() => refreshCount(), POLL_INTERVAL_MS);
  }

  function stopPolling() {
    if (pollTimer) {
      clearInterval(pollTimer);
      pollTimer = null;
    }
  }

  return {
    notifications,
    unreadCount,
    loading,
    refresh,
    refreshCount,
    markRead,
    markAllRead,
    remove,
    startPolling,
    stopPolling,
  };
});
