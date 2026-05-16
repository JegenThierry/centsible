import axios from 'axios'

export default defineNuxtPlugin((nuxtApp) => {
  const config = useRuntimeConfig();
  const baseURL = import.meta.server ? config.apiBaseSSR : config.public.apiBase;

  const api = axios.create({
    baseURL: baseURL as string,
    // Required so the browser attaches the HttpOnly auth cookie on cross-origin XHRs.
    withCredentials: true,
    headers: {
      common: {}
    }
  })

  // Tell the backend which language to localize validation messages / exceptions /
  // emails in. The header is re-read per request so it tracks live locale changes.
  api.interceptors.request.use((cfg) => {
    const i18n = nuxtApp.$i18n as {locale?: {value?: string}} | undefined;
    const locale = i18n?.locale?.value;
    if (locale) {
      cfg.headers.set('Accept-Language', locale);
    }
    return cfg;
  });

  // During SSR the request to the backend is server-to-server, so the browser's cookies
  // are NOT auto-attached. Forward the incoming request's cookie header so the auth-guard's
  // /auth/verify call carries the user's auth_token and the session survives hard refreshes.
  if (import.meta.server) {
    const requestHeaders = useRequestHeaders(['cookie']);
    api.interceptors.request.use((cfg) => {
      if (requestHeaders.cookie) {
        cfg.headers.set('Cookie', requestHeaders.cookie);
      }
      return cfg;
    });
  }

  api.interceptors.response.use(
    (response) => response,
    (error) => Promise.reject(error)
  );

  return {
    provide: {
      axios: api
    }
  }
})
