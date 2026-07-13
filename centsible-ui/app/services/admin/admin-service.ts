import type {AxiosInstance} from "axios";
import type {AdminUser} from "~/models/admin/admin-user";
import {assertStatus, validateRequest} from "~/composables/use-api";

export function useAdminService(api: AxiosInstance) {
  async function fetchUsers(): Promise<AdminUser[]> {
    const response = await api.get<AdminUser[]>('/admin/users');
    return validateRequest(response);
  }

  async function deleteUser(id: string): Promise<void> {
    const response = await api.delete(`/admin/users/${id}`);
    assertStatus(response);
  }

  async function resendVerification(id: string): Promise<void> {
    const response = await api.post(`/admin/users/${id}/resend-verification`);
    assertStatus(response);
  }

  return {
    fetchUsers,
    deleteUser,
    resendVerification,
  }
}
