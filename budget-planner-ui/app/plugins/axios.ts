import axios from 'axios'
import {useAuthStore} from "~/stores/authStore";

export default defineNuxtPlugin(() => {
    const config = useRuntimeConfig();
    const baseURL = process.server ? config.apiBaseSsr : config.public.apiBase;

console.log('apiBase:', config.public.apiBase)
console.log('apiBaseSSR:', config.apiBaseSSR)

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
        (error) => {
            // if (error.response && error.response.status === 401) {
            //     navigateTo("/auth")
            // }
            return Promise.reject(error);
        }
    );

    return {
        provide: {
            axios: api
        }
    }
})
