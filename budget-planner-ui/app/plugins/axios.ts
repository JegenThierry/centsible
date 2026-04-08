import axios from 'axios'
import {useAuthStore} from "~/stores/authStore";
import {unref} from "vue";

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
      // In Pinia setup stores, state is unwrapped.
      // But we can use unref to be safe across different environments.
      const token = unref(authStore.token);

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
