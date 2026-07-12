/** Reactive BCP-47 language tag for the active locale, falling back to the locale code. */
export function useLocaleTag() {
  const {locale, locales} = useI18n();
  return computed(() => {
    const match = (locales.value as Array<{code: string; language?: string}>)
      .find(l => l.code === locale.value);
    return match?.language ?? locale.value;
  });
}
