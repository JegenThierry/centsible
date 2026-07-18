import type {Pinia, PiniaPluginContext} from 'pinia'

/**
 * Gives every store — including setup stores, which Pinia does not grant a working `$reset` — a reset
 * back to the state it held right after its setup ran, before any fetch populated it. We snapshot that
 * state on store creation and `$patch` it back on demand.
 *
 * The snapshot is a structured clone so a later mutation of a nested array/object in the store can't
 * reach back and corrupt the baseline; the clone is guarded because a store could hold a value the
 * structured-clone algorithm rejects, in which case a shallow copy is a good-enough fallback.
 *
 * {@link resetAllStores} drives this on sign-out, where login/logout never reload the tab and stale
 * state would otherwise bleed across sessions.
 */
export default defineNuxtPlugin((nuxtApp) => {
  const pinia = nuxtApp.$pinia as Pinia
  pinia.use(({store}: PiniaPluginContext) => {
    const initialState = safeClone(toRaw(store.$state))
    store.$reset = () => store.$patch(($state) => Object.assign($state, safeClone(initialState)))
  })
})

function safeClone<T>(value: T): T {
  try {
    return structuredClone(value)
  } catch {
    return {...value}
  }
}
