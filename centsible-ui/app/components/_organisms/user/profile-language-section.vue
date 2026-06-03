<script lang="ts" setup>
import AppSelectMenu from '~/components/_atoms/ui/app-select-menu.vue';

const emit = defineEmits<{
  (e: 'change', code: string): void;
}>();

const {t, locale, locales} = useI18n();

const languageOptions = computed(() =>
  (locales.value as Array<{code: string}>).map((l) => ({
    label: t(`profile.language.${l.code}`),
    value: l.code,
  })),
);

function onLanguageChange(code: string) {
  emit('change', code);
}
</script>

<template>
  <div>
    <h3 class="text-lg font-semibold">{{ t('profile.language.title') }}</h3>
    <p class="text-sm text-muted mt-1">{{ t('profile.language.description') }}</p>
    <AppSelectMenu
      class="mt-4 w-full"
      :model-value="locale"
      :items="languageOptions"
      label-key="label"
      value-key="value"
      @update:model-value="onLanguageChange"
    />
  </div>
</template>
