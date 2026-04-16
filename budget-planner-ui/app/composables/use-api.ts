import {useNuxtApp} from "nuxt/app";
import type {AxiosResponse} from "axios";

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
