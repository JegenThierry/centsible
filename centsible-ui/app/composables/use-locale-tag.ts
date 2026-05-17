export function useLocaleTag() {
  const {locale, locales} = useI18n();
  return computed(() => {
    const match = (locales.value as Array<{code: string; language?: string}>)
      .find(l => l.code === locale.value);
    return match?.language ?? locale.value;
  });
}
