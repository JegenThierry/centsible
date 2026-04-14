import {defineStore} from 'pinia'
import type {UserDto} from "~/models/user/user-dto";
import {useUserService} from "~/services/user/user-service";

export const useUserStore = defineStore('userStore', () => {
  const api = useApi();
  const userService = useUserService(api);

  const user = ref<UserDto | null>(null);

  async function fetchMyself() {
    user.value = await userService.fetchMyself();
  }

  async function updateProfile(profile: { firstName: string, lastName: string, email: string }) {
      user.value = await userService.updateProfile(profile);
  }

  async function updateProfilePicture(file: File) {
      user.value = await userService.updateProfilePicture(file);
  }

  return {
    user,
    fetchMyself,
    updateProfile,
    updateProfilePicture,
  }
});
