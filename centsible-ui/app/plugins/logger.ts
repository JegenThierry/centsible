import { setup } from 'adze'

export default defineNuxtPlugin(() => {
  setup({
    activeLevel: import.meta.dev ? 'verbose' : 'info',
    format: 'pretty',
  })
})
