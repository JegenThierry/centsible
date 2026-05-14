// https://nuxt.com/docs/api/configuration/nuxt-config
export default defineNuxtConfig({
  compatibilityDate: '2025-07-15',
  devtools: {enabled: false},
  modules: ['@nuxt/ui', '@pinia/nuxt'],
  css: ['@/assets/css/main.css'],
  vite: {
    optimizeDeps: {
      include: [
        'axios',
        '@vueuse/core',
      ]
    }
  },
  runtimeConfig: {
    apiBaseSSR: 'http://budget-planner-rest:8080/api',
    public: {
      apiBase: "http://localhost:8080/api",
    }
  },
  app: {
    head: {
      title: 'Budget Planner',
    }
  }
})
