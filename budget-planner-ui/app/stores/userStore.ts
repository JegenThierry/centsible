import {defineStore} from 'pinia'
import type {UserDto} from "~/models/user/user-dto";
import {useUserService} from "~/services/user/user-service";
import type {UserProfileForm} from "~/models/user/user-profile-form";

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

  function clear() {
    user.value = null;
    pending.value = false;
  }

  return {
    user,
    pending,
    fetchMyself,
    updateProfile,
    updateProfilePicture,
    clear,
  }
});
