// https://nuxt.com/docs/api/configuration/nuxt-config
export default defineNuxtConfig({
    compatibilityDate: '2025-07-15',
    devtools: {enabled: false},
    modules: ['@nuxt/ui', '@pinia/nuxt'],
    css: ['@/assets/css/main.css'],
    vite: {
        optimizeDeps: {
            include: [
                'axios'
            ]
        }
    },
    runtimeConfig: {
        public: {
            apiBase: process.env.NUXT_PUBLIC_API_BASE || "http://localhost:8080/api",
        }
    }
})