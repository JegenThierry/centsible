import type {AxiosInstance} from "axios";
import type {UserDto} from "~/models/user/user-dto";
import type {NotificationSettings} from "~/models/notification/notification-settings";
import type {Currency} from "~/models/budget-account/currency";
import {postMultipart, validateRequest} from "~/composables/use-api";

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
    return postMultipart<UserDto>(api, '/users/profile/picture', {file});
  }

  async function updateLocale(locale: string): Promise<UserDto> {
    const response = await api.put<UserDto>('/users/locale', {locale});
    return validateRequest(response);
  }

  async function updateDefaultCurrency(defaultCurrency: Currency): Promise<UserDto> {
    const response = await api.put<UserDto>('/users/default-currency', {defaultCurrency});
    return validateRequest(response);
  }

  async function fetchNotificationSettings(): Promise<NotificationSettings> {
    const response = await api.get<NotificationSettings>('/users/notification-settings');
    return validateRequest(response);
  }

  async function updateNotificationSettings(settings: NotificationSettings): Promise<NotificationSettings> {
    const response = await api.put<NotificationSettings>('/users/notification-settings', settings);
    return validateRequest(response);
  }

  return {
    fetchMyself,
    updateProfile,
    updateProfilePicture,
    updateLocale,
    updateDefaultCurrency,
    fetchNotificationSettings,
    updateNotificationSettings,
  }
}
