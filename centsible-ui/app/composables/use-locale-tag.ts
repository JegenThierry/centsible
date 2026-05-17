/**
 * Returns a reactive BCP-47 tag (e.g. `en-US`, `fr-FR`, `de-DE`) for the
 * currently active i18n locale. Use it whenever you need a tag for
 * `Intl.*` formatters that should follow the user's language choice.
 */
export function useLocaleTag() {
  const {locale, locales} = useI18n();
  return computed(() => {
    const match = (locales.value as Array<{code: string; language?: string}>)
      .find(l => l.code === locale.value);
    return match?.language ?? locale.value;
  });
}
