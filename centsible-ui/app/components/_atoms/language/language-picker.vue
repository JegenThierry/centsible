<script lang="ts" setup>
const {locale, locales, t} = useI18n();
const {apply} = useLocaleSwitcher();

const items = computed(() =>
  (locales.value as Array<{code: string; name: string}>).map(l => ({
    label: l.name,
    onSelect: () => apply(l.code),
  }))
);

const currentLabel = computed(() => {
  const match = (locales.value as Array<{code: string; name: string}>)
    .find(l => l.code === locale.value);
  return match?.name ?? locale.value;
});
</script>

<template>
  <UDropdownMenu :items="items">
    <UButton :aria-label="t('common.language.label')"
             color="neutral"
             icon="i-lucide-languages"
             size="sm"
             variant="ghost">
      <span class="hidden sm:inline">{{ currentLabel }}</span>
    </UButton>
  </UDropdownMenu>
</template>
