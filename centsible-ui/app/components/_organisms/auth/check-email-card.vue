<script lang="ts" setup>
import AppButton from "~/components/_atoms/ui/app-button.vue";

const {t} = useI18n();

const route = useRoute();
const email = computed(() => {
  const value = route.query.email;
  return typeof value === 'string' ? value : undefined;
});

const description = computed(() =>
  email.value
    ? t('auth.checkEmail.descriptionWithEmail', {email: email.value})
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
          <AppButton color="neutral"
                   icon="i-lucide-home"
                   size="lg"
                   to="/"
                   variant="outline">
            {{ t('auth.checkEmail.backToHome') }}
          </AppButton>
          <AppButton color="primary"
                   icon="i-lucide-log-in"
                   size="lg"
                   to="/auth"
                   trailing-icon="i-lucide-arrow-right">
            {{ t('auth.checkEmail.backToSignIn') }}
          </AppButton>
        </div>
      </template>
    </UPageCard>
  </UContainer>
</template>
