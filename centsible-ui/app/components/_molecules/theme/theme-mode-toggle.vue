<script lang="ts" setup>
import type {ThemeMode} from "~/composables/use-theme";

defineProps<{
  modelValue: ThemeMode;
}>();

defineEmits<{
  'update:modelValue': [value: ThemeMode];
}>();

const {t} = useI18n();

const modes = computed<Array<{value: ThemeMode; label: string; icon: string}>>(() => [
  {value: 'light', label: t('profile.theme.modes.light'), icon: 'i-lucide-sun'},
  {value: 'dark', label: t('profile.theme.modes.dark'), icon: 'i-lucide-moon'},
  {value: 'system', label: t('profile.theme.modes.system'), icon: 'i-lucide-monitor'},
]);
</script>

<template>
  <UTabs :content="false"
         :items="modes"
         :model-value="modelValue"
         :ui="{indicator: 'bg-default ring-1 ring-default', trigger: 'data-[state=active]:text-highlighted'}"
         color="neutral"
         size="sm"
         @update:model-value="$emit('update:modelValue', $event as ThemeMode)"/>
</template>
