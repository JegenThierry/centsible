import {defineStore} from 'pinia'
import type {UserDto} from "~/models/user/user-dto";
import {useUserService} from "~/services/user/user-service";

export const useUserStore = defineStore('userStore', () => {
  const api = useApi();
  const userService = useUserService(api);

  const user = ref<UserDto | null>(null);
  const pending = ref(false);

  async function fetchMyself() {
    pending.value = true;
    try {
      user.value = await userService.fetchMyself();
    } catch (e) {
      console.error("Failed to fetch user data", e);
      user.value = null;
    } finally {
      pending.value = false;
    }
  }

  async function updateProfile(profile: { firstName: string, lastName: string, email: string }) {
    pending.value = true;
    try {
      user.value = await userService.updateProfile(profile);
    } catch (e) {
      console.error("Failed to update profile", e);
      throw e;
    } finally {
      pending.value = false;
    }
  }

  async function updateProfilePicture(file: File) {
    pending.value = true;
    try {
      user.value = await userService.updateProfilePicture(file);
    } catch (e) {
      console.error("Failed to update profile picture", e);
      throw e;
    } finally {
      pending.value = false;
    }
  }

  return {
    user,
    pending,
    fetchMyself,
    updateProfile,
    updateProfilePicture,
  }
});
