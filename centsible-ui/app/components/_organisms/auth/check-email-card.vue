<script lang="ts" setup>
const props = defineProps<{
  email?: string;
}>();

const {t} = useI18n();
const description = computed(() =>
  props.email
    ? t('auth.checkEmail.descriptionWithEmail', {email: props.email})
    : t('auth.checkEmail.descriptionGeneric')
);
</script>

<template>
  <UContainer class="py-12">
    <UPageCard
      :description="description"
      :ui="{ leadingIcon: 'text-primary' }"
      class="max-w-xl mx-auto"
      icon="i-lucide-mail-check"
      spotlight
      spotlight-color="primary"
      :title="t('auth.checkEmail.title')">
      <p class="text-sm text-(--ui-text-muted)">
        {{ t('auth.checkEmail.expiryNote') }}
      </p>

      <template #footer>
        <div class="flex flex-col sm:flex-row gap-3 sm:justify-end">
          <UButton color="neutral"
                   icon="i-lucide-home"
                   size="lg"
                   to="/"
                   variant="outline">
            {{ t('auth.checkEmail.backToHome') }}
          </UButton>
          <UButton color="primary"
                   icon="i-lucide-log-in"
                   size="lg"
                   to="/auth"
                   trailing-icon="i-lucide-arrow-right">
            {{ t('auth.checkEmail.backToSignIn') }}
          </UButton>
        </div>
      </template>
    </UPageCard>
  </UContainer>
</template>
