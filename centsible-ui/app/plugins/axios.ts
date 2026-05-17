import axios from 'axios'

export default defineNuxtPlugin((nuxtApp) => {
  const config = useRuntimeConfig();
  const baseURL = import.meta.server ? config.apiBaseSSR : config.public.apiBase;

  const api = axios.create({
    baseURL: baseURL as string,
    withCredentials: true,
  });

  api.interceptors.request.use((cfg) => {
    const i18n = nuxtApp.$i18n as {locale?: {value?: string}} | undefined;
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

  return {
    provide: {
      axios: api
    }
  }
})
