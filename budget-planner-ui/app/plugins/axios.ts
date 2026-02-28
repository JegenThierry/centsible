import axios from 'axios'
import {useAuthStore} from "~/stores/auth";

export default defineNuxtPlugin(() => {
    const config = useRuntimeConfig();

    const api = axios.create({
        baseURL: config.public.apiBase as string,
        headers: {
            common: {}
        }
    })

    api.interceptors.request.use((config) => {
        const {token} = useAuthStore();
        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
    });

    api.interceptors.response.use(
        (response) => response,
        (error) => {
            if (error.response && error.response.status === 401) {
                navigateTo("/login")
            }
            return Promise.reject(error);
        }
    );

    return {
        provide: {
            axios: api
        }
    }
})