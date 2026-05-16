import type {AxiosInstance} from "axios";
import type {UserDto} from "~/models/user/user-dto";
import {validateRequest} from "~/composables/use-api";

export function useUserService(api: AxiosInstance) {
  async function fetchMyself(): Promise<UserDto> {
    const response = await api.get<UserDto>('/users/myself');
    return validateRequest(response);
  }

  async function updateProfile(profile: { firstName: string, lastName: string, email: string }): Promise<UserDto> {
    const response = await api.put<UserDto>('/users/profile', profile);
    return validateRequest(response);
  }

  async function updateProfilePicture(file: File): Promise<UserDto> {
    const formData = new FormData();
    formData.append('file', file);
    const response = await api.post<UserDto>('/users/profile/picture', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    });
    return validateRequest(response);
  }

  async function updateLocale(locale: string): Promise<UserDto> {
    const response = await api.put<UserDto>('/users/locale', {locale});
    return validateRequest(response);
  }

  return {
    fetchMyself,
    updateProfile,
    updateProfilePicture,
    updateLocale,
  }
}
