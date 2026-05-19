// https://nuxt.com/docs/api/configuration/nuxt-config
export default defineNuxtConfig({
  compatibilityDate: '2025-07-15',
  devtools: {enabled: false},
  modules: ['@nuxt/ui', '@pinia/nuxt', '@nuxtjs/i18n'],
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
    apiBaseSSR: 'http://centsible-rest:8080/api',
    public: {
      apiBase: "http://localhost:8080/api",
    }
  },
  i18n: {
    strategy: 'no_prefix',
    defaultLocale: 'en',
    locales: [
      {code: 'en', name: 'English', language: 'en-US', files: ['en/common.json', 'en/nav.json', 'en/auth.json', 'en/landing.json', 'en/accounts.json', 'en/transactions.json', 'en/budgets.json', 'en/categories.json', 'en/contacts.json', 'en/profile.json', 'en/exports.json', 'en/integrations.json', 'en/notifications.json', 'en/onboarding.json', 'en/reports.json', 'en/attachments.json']},
      {code: 'fr', name: 'Français', language: 'fr-FR', files: ['fr/common.json', 'fr/nav.json', 'fr/auth.json', 'fr/landing.json', 'fr/accounts.json', 'fr/transactions.json', 'fr/budgets.json', 'fr/categories.json', 'fr/contacts.json', 'fr/profile.json', 'fr/exports.json', 'fr/integrations.json', 'fr/notifications.json', 'fr/onboarding.json', 'fr/reports.json', 'fr/attachments.json']},
      {code: 'de', name: 'Deutsch', language: 'de-DE', files: ['de/common.json', 'de/nav.json', 'de/auth.json', 'de/landing.json', 'de/accounts.json', 'de/transactions.json', 'de/budgets.json', 'de/categories.json', 'de/contacts.json', 'de/profile.json', 'de/exports.json', 'de/integrations.json', 'de/notifications.json', 'de/onboarding.json', 'de/reports.json', 'de/attachments.json']},
    ],
    lazy: true,
    bundle: {
      optimizeTranslationDirective: false,
    },
    detectBrowserLanguage: {
      useCookie: true,
      cookieKey: 'centsible_locale',
      redirectOn: 'root',
      alwaysRedirect: false,
      fallbackLocale: 'en',
    },
  },
  app: {
    head: {
      title: 'Centsible',
      meta: [
        {charset: 'utf-8'},
        {name: 'viewport', content: 'width=device-width, initial-scale=1'},
        {
          name: 'description',
          content: 'Centsible is a clean, self-hosted budget tracker with multiple accounts, smart categories, monthly budgets, and dashboards that actually help you decide.',
        },
        {name: 'theme-color', content: '#ee387e'},
        {name: 'color-scheme', content: 'light dark'},
        {property: 'og:type', content: 'website'},
        {property: 'og:site_name', content: 'Centsible'},
        {property: 'og:title', content: 'Centsible — Take control of your money'},
        {
          property: 'og:description',
          content: 'A clean, self-hosted budget tracker. Open source, no tracking, no ads.',
        },
        {property: 'og:image', content: '/brand/og-image.png'},
        {property: 'og:image:width', content: '1200'},
        {property: 'og:image:height', content: '630'},
        {name: 'twitter:card', content: 'summary_large_image'},
        {name: 'twitter:title', content: 'Centsible — Take control of your money'},
        {
          name: 'twitter:description',
          content: 'A clean, self-hosted budget tracker. Open source, no tracking, no ads.',
        },
        {name: 'twitter:image', content: '/brand/og-image.png'},
      ],
      link: [
        {rel: 'icon', type: 'image/svg+xml', href: '/brand/favicon.svg'},
        {rel: 'icon', type: 'image/x-icon', href: '/favicon.ico'},
        {rel: 'icon', type: 'image/png', sizes: '32x32', href: '/brand/icon-32.png'},
        {rel: 'apple-touch-icon', sizes: '180x180', href: '/brand/apple-touch-icon.png'},
        {rel: 'manifest', href: '/site.webmanifest'},
      ],
    }
  }
})
