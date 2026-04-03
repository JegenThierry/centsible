import {defineStore} from 'pinia'
import type {UserDto} from "~/models/user/user-dto";
import {useUserService} from "~/services/user/user-service";

export const useUserStore = defineStore('userStore', () => {
  const api = useApi();
  const userService = useUserService(api);

  const user = ref<UserDto | null>(null);

  async function fetchMyself() {
    try {
      user.value = await userService.fetchMyself();
    } catch (e) {
      console.error("Failed to fetch user data", e);
      user.value = null;
    }
  }

  return {
    user,
    fetchMyself,
  }
});
