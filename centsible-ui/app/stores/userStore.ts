import {defineStore} from 'pinia'
import adze from 'adze'
import type {UserDto} from "~/models/user/user-dto";
import {useUserService} from "~/services/user/user-service";
import type {UserProfileForm} from "~/models/user/user-profile-form";
import type {Currency} from "~/models/budget-account/currency";

export const useUserStore = defineStore('userStore', () => {
  const api = useApi();
  const userService = useUserService(api);

  const user = ref<UserDto | null>(null);
  const pending = ref(false);

  async function fetchMyself() {
    pending.value = true;
    try {
      user.value = await userService.fetchMyself();
    } finally {
      pending.value = false;
    }
  }

  /**
   * Profile load for the post-authentication path, where the session is already established and the
   * caller is about to redirect. A failure here must not gate that redirect: the destination re-fetches
   * on mount, and a genuine 401 is handled by the axios interceptor. Callers that need the profile to
   * be authoritative (e.g. admin-guard) must use `fetchMyself` and handle the rejection themselves.
   */
  async function fetchMyselfBestEffort() {
    try {
      await fetchMyself();
    } catch (error) {
      adze.ns('user').warn('Profile fetch failed after authentication; continuing.', error);
    }
  }

  async function updateProfile(profile: UserProfileForm) {
    pending.value = true;
    try {
      user.value = await userService.updateProfile(profile);
    } finally {
      pending.value = false;
    }
  }

  async function updateProfilePicture(file: File) {
    pending.value = true;
    try {
      user.value = await userService.updateProfilePicture(file);
    } finally {
      pending.value = false;
    }
  }

  async function updateLocale(locale: string) {
    user.value = await userService.updateLocale(locale);
  }

  async function updateDefaultCurrency(currency: Currency) {
    user.value = await userService.updateDefaultCurrency(currency);
  }

  function clear() {
    user.value = null;
    pending.value = false;
  }

  return {
    user,
    pending,
    fetchMyself,
    fetchMyselfBestEffort,
    updateProfile,
    updateProfilePicture,
    updateLocale,
    updateDefaultCurrency,
    clear,
  }
});
