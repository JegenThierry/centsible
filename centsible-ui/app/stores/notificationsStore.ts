import {defineStore} from 'pinia';
import adze from 'adze'
import {useNotificationService} from "~/services/notifications/notification-service";
import type {Notification} from "~/models/notification/notification";
import {fingerprint} from "~/utils/fingerprint";

const POLL_INTERVAL_MS = 60_000;

const notificationFingerprint = (xs: Notification[]) =>
  fingerprint(xs, (n) => `${n.id}|${n.readAt ?? ''}`);

export const useNotificationsStore = defineStore('notificationsStore', () => {
  const service = useNotificationService(useApi());
  const apiErrors = useApiErrors();
  const {t} = useNuxtApp().$i18n;
  const notifications = ref<Notification[]>([]);
  const unreadCount = ref(0);
  const loading = ref(false);

  async function refresh() {
    loading.value = true;
    try {
      const next = await service.list(50);
      if (notificationFingerprint(next) !== notificationFingerprint(notifications.value)) {
        notifications.value = next;
      }
      unreadCount.value = next.filter((n) => !n.readAt).length;
    } catch (e) {
      adze.ns('notifications').error('Failed to load notifications', e);
    } finally {
      loading.value = false;
    }
  }

  async function refreshCount() {
    try {
      unreadCount.value = await service.unreadCount();
    } catch {
    }
  }

  function toastUpdateFailed(e: unknown) {
    apiErrors.toastError(e, t('notifications.toasts.updateFailedTitle'), t('notifications.toasts.updateFailedBody'));
  }

  async function markRead(id: string) {
    try {
      await service.markRead(id);
    } catch (e) {
      toastUpdateFailed(e);
      return;
    }
    const n = notifications.value.find((x) => x.id === id);
    if (n && !n.readAt) {
      n.readAt = new Date().toISOString();
      unreadCount.value = Math.max(0, unreadCount.value - 1);
    }
  }

  async function markAllRead() {
    try {
      await service.markAllRead();
    } catch (e) {
      toastUpdateFailed(e);
      return;
    }
    const now = new Date().toISOString();
    notifications.value.forEach((n) => {
      if (!n.readAt) n.readAt = now;
    });
    unreadCount.value = 0;
  }

  async function remove(id: string) {
    try {
      await service.remove(id);
    } catch (e) {
      toastUpdateFailed(e);
      return;
    }
    const idx = notifications.value.findIndex((n) => n.id === id);
    if (idx >= 0) {
      const [removed] = notifications.value.splice(idx, 1);
      if (removed && !removed.readAt) unreadCount.value = Math.max(0, unreadCount.value - 1);
    }
  }

  const {resume: startPolling, pause: stopPolling} = useIntervalFn(
    () => refreshCount(),
    POLL_INTERVAL_MS,
    {immediate: false},
  );

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
