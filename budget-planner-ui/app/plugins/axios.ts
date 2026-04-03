import axios from 'axios'
import {useAuthStore} from "~/stores/authStore";

export default defineNuxtPlugin(() => {
  const config = useRuntimeConfig();
  const baseURL = import.meta.server ? config.apiBaseSSR : config.public.apiBase;

  const api = axios.create({
    baseURL: baseURL as string,
    headers: {
      common: {}
    }
  })

  const authStore = useAuthStore();
  api.interceptors.request.use((config) => {
    try {
      const token = authStore.token;
      if (token) {
        config.headers.Authorization = `Bearer ${token}`;
      }
    } catch (e) {
      console.error('Failed to get token from authStore in interceptor', e);
    }
    return config;
  });

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
