import type {AxiosInstance} from "axios";
import {assertStatus, validateRequest} from "~/composables/use-api";
import type {Notification} from "~/models/notification/notification";

export function useNotificationService(api: AxiosInstance) {
  async function list(limit: number = 50): Promise<Notification[]> {
    const response = await api.get<Notification[]>('/notifications', {params: {limit}});
    return validateRequest<Notification[]>(response);
  }

  async function unreadCount(): Promise<number> {
    const response = await api.get<{count: number}>('/notifications/unread-count');
    return validateRequest<{count: number}>(response).count;
  }

  async function markRead(id: string): Promise<void> {
    assertStatus(await api.post(`/notifications/${encodeURIComponent(id)}/read`));
  }

  async function markAllRead(): Promise<number> {
    const response = await api.post<{affected: number}>('/notifications/read-all');
    return validateRequest<{affected: number}>(response).affected;
  }

  async function remove(id: string): Promise<void> {
    assertStatus(await api.delete(`/notifications/${encodeURIComponent(id)}`));
  }

  return {list, unreadCount, markRead, markAllRead, remove};
}
