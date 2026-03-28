import {useAuthStore} from "~/stores/authStore";

export default defineNuxtRouteMiddleware((to) => {
    const authStore = useAuthStore();

    if (!authStore.isAuthenticated && to.path !== '/auth') {
        return navigateTo('/auth')
    }
})