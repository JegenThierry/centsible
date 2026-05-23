import adze from 'adze'
import {useUserStore} from "~/stores/userStore";
import {useAuthStore} from "~/stores/authStore";

type SupportedLocale = 'en' | 'fr' | 'de';

export function useLocaleSwitcher() {
  const {locale, setLocale} = useI18n();
  const userStore = useUserStore();
  const authStore = useAuthStore();

  async function apply(code: string) {
    if (code === locale.value) return;
    await setLocale(code as SupportedLocale);
    if (!authStore.isAuthenticated) return;
    await userStore.updateLocale(code).catch((error) => adze.ns('auth').warn('Failed to persist locale; cookie keeps choice', error));
  }

  return {apply};
}
