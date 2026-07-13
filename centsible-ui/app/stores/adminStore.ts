import {defineStore} from "pinia";
import type {AdminUser} from "~/models/admin/admin-user";
import {useAdminService} from "~/services/admin/admin-service";
import {useToasts} from "~/services/toasts/toast-service";
import {useApiErrors} from "~/composables/use-api-errors";

export const useAdminStore = defineStore('adminStore', () => {
  const api = useApi();
  const toasts = useToasts();
  const apiErrors = useApiErrors();
  const {t} = useNuxtApp().$i18n;
  const adminService = useAdminService(api);

  const users = ref<AdminUser[]>([]);
  const pending = ref(false);
  const error = ref(false);

  async function fetchUsers() {
    pending.value = true;
    error.value = false;
    try {
      users.value = await adminService.fetchUsers();
    } catch (err) {
      error.value = true;
      apiErrors.toastError(err, t('admin.toasts.fetchFailedTitle'), t('admin.toasts.fetchFailedBody'));
      throw err;
    } finally {
      pending.value = false;
    }
  }

  async function deleteUser(id: string) {
    pending.value = true;
    try {
      await adminService.deleteUser(id);
      toasts.success(t('admin.toasts.deletedTitle'), t('admin.toasts.deletedBody'));
      await fetchUsers();
    } catch (err) {
      apiErrors.toastError(err, t('admin.toasts.deleteFailedTitle'), t('admin.toasts.genericErrorBody'));
      throw err;
    } finally {
      pending.value = false;
    }
  }

  async function resendVerification(id: string) {
    try {
      await adminService.resendVerification(id);
      toasts.success(t('admin.toasts.resendTitle'), t('admin.toasts.resendBody'));
    } catch (err) {
      apiErrors.toastError(err, t('admin.toasts.resendFailedTitle'), t('admin.toasts.genericErrorBody'));
      throw err;
    }
  }

  return {
    users,
    pending,
    error,
    fetchUsers,
    deleteUser,
    resendVerification,
  }
});
