<script lang="ts" setup>
import AppButton from "~/components/_atoms/ui/app-button.vue";

const emit = defineEmits<{
  (e: 'finish'): void;
}>();

const {t} = useI18n();
</script>

<template>
  <div class="space-y-4 text-center">
    <UIcon class="w-12 h-12 mx-auto text-success" name="i-lucide-check-circle"/>
    <h2 class="text-xl font-semibold">{{ t('onboarding.done.title') }}</h2>
    <p class="text-sm text-muted">{{ t('onboarding.done.description') }}</p>
    <AppButton color="primary"
               size="lg"
               trailing-icon="i-lucide-layout-dashboard"
               @click="emit('finish')">
      {{ t('onboarding.done.cta') }}
    </AppButton>
  </div>
</template>
