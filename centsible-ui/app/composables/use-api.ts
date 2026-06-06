import {useNuxtApp} from "nuxt/app";
import type {AxiosInstance, AxiosResponse} from "axios";

export const useApi = () => {
  const nuxtApp = useNuxtApp()
  return nuxtApp.$axios;
}

export function validateRequest<T>(request: AxiosResponse<T>): T {
  if (request.status === 200 && request.data) {
    return request.data;
  }

  throw new Error(request.statusText);
}

/**
 * Throws unless the status is one of the accepted values. Use for endpoints that legitimately
 * return either 200 or 204 (or 202 for accepted-but-async) and don't carry a body we need.
 */
export function assertStatus(response: AxiosResponse, accepted: number[] = [200, 204]): void {
  if (!accepted.includes(response.status)) {
    throw new Error(response.statusText);
  }
}

export async function postMultipart<T>(
  api: AxiosInstance,
  url: string,
  fields: Record<string, string | Blob>,
): Promise<T> {
  const form = new FormData();
  for (const [key, value] of Object.entries(fields)) {
    form.append(key, value);
  }
  const response = await api.post<T>(url, form, {
    headers: {'Content-Type': 'multipart/form-data'},
  });
  return validateRequest<T>(response);
}
