// The per-feature locale files, loaded for every locale (explicit list, not a filesystem glob).
const i18nFiles = [
  'common.json', 'nav.json', 'auth.json', 'landing.json', 'accounts.json', 'transactions.json',
  'budgets.json', 'categories.json', 'rules.json', 'tags.json', 'contacts.json', 'profile.json',
  'exports.json', 'integrations.json', 'notifications.json', 'onboarding.json', 'reports.json',
  'attachments.json', 'admin.json',
];

const i18nLocales = [
  {code: 'en', name: 'English', language: 'en-US'},
  {code: 'fr', name: 'Français', language: 'fr-FR'},
  {code: 'de', name: 'Deutsch', language: 'de-DE'},
].map(locale => ({...locale, files: i18nFiles.map(file => `${locale.code}/${file}`)}));

export default defineNuxtConfig({
  compatibilityDate: '2025-07-15',
  devtools: {enabled: false},
  modules: ['@nuxt/ui', '@pinia/nuxt', '@nuxtjs/i18n', '@vueuse/nuxt'],
  css: ['@/assets/css/main.css'],
  // No manualChunks for chart.js: naming it as one made Rollup merge it into the always-loaded
  // shared chunk, costing every route ~67 kB gzip. Default splitting isolates it correctly.
  vite: {
    optimizeDeps: {
      include: [
        'axios',
        '@vueuse/core',
      ]
    },
  },
  runtimeConfig: {
    apiBaseSSR: 'http://localhost:8080/api',
    public: {
      apiBase: "http://localhost:8080/api",
    }
  },
  i18n: {
    strategy: 'no_prefix',
    defaultLocale: 'en',
    locales: i18nLocales,
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
