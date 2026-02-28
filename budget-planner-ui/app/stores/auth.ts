import {defineStore} from 'pinia'

const ONE_WEEK = 60 * 60 * 24 * 7;

export const useAuthStore = defineStore(
    'auth',
    () => {
        const token = useCookie('auth_token', {
            maxAge: ONE_WEEK,
            watch: true
        })

        const isAuthenticated = computed(() => !!token.value)

        function setToken(newToken: string) {
            token.value = newToken
        }

        function logout() {
            token.value = null;
            navigateTo('/login')
        }

        return {
            isAuthenticated,
            token,
            logout,
            setToken,
        }
    }
);