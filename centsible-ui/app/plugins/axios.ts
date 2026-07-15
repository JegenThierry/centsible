import axios from 'axios'
import {useAuthStore} from "~/stores/authStore";
import {useUserStore} from "~/stores/userStore";

export default defineNuxtPlugin((nuxtApp) => {
  const config = useRuntimeConfig();
  const baseURL = import.meta.server ? config.apiBaseSSR : config.public.apiBase;

  const api = axios.create({
    baseURL: baseURL as string, withCredentials: true,
  });

  api.interceptors.request.use((cfg) => {
    const i18n = nuxtApp.$i18n as { locale?: { value?: string } } | undefined;
    const locale = i18n?.locale?.value;
    if (locale) {
      cfg.headers.set('Accept-Language', locale);
    }
    return cfg;
  });

  if (import.meta.server) {
    const requestHeaders = useRequestHeaders(['cookie']);
    api.interceptors.request.use((cfg) => {
      if (requestHeaders.cookie) {
        cfg.headers.set('Cookie', requestHeaders.cookie);
      }
      return cfg;
    });
  }

  api.interceptors.response.use((response) => response, (error) => {
    const axiosError = error as { response?: { status?: number }; config?: { url?: string } };
    if (import.meta.client && axiosError.response?.status === 401) {
      const requestUrl = axiosError.config?.url ?? '';
      if (!requestUrl.startsWith('/auth')) {
        nuxtApp.runWithContext(() => {
          const authStore = useAuthStore();
          if (authStore.isAuthenticated) {
            authStore.setAuthenticated(false);
            useUserStore().clear();
            // Only effective outside route middleware: while middleware is processing, navigateTo
            // returns the route without navigating. Guards that await a request therefore re-check
            // `isAuthenticated` after the await and own the redirect themselves.
            return navigateTo('/auth');
          }
        });
      }
    }
    return Promise.reject(error);
  });

  return {
    provide: {
      axios: api
    }
  }
})
