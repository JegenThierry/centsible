import {useUserStore} from "~/stores/userStore";
import {useAuthStore} from "~/stores/authStore";

type SupportedLocale = 'en' | 'fr' | 'de';

/**
 * Switches the active runtime locale, then best-effort-persists it as the user's
 * stored preference. Backend write is silently swallowed because the cookie
 * (managed by `@nuxtjs/i18n`) already preserves the choice locally.
 */
export function useLocaleSwitcher() {
  const {locale, setLocale} = useI18n();
  const userStore = useUserStore();
  const authStore = useAuthStore();

  async function apply(code: string) {
    if (code === locale.value) return;
    await setLocale(code as SupportedLocale);
    if (authStore.isAuthenticated) {
      await userStore.updateLocale(code).catch(() => { /* cookie keeps choice */ });
    }
  }

  return {apply};
}
