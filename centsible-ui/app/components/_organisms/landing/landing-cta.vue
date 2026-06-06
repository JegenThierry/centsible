<script lang="ts" setup>
import AppButton from "~/components/_atoms/ui/app-button.vue";

const {t} = useI18n();
</script>

<template>
  <section class="pb-20 sm:pb-28">
    <UContainer>
      <UPageCard
        class="bg-gradient-to-br from-primary/10 via-primary/5 to-transparent border border-default"
        :description="t('landing.cta.description')"
        spotlight
        spotlight-color="primary"
        :title="t('landing.cta.title')">
        <template #footer>
          <div class="flex flex-col sm:flex-row gap-3 sm:justify-end">
            <AppButton color="primary"
                       icon="i-lucide-user-plus"
                       size="lg"
                       to="/auth"
                       trailing-icon="i-lucide-arrow-right">
              {{ t('landing.cta.createAccount') }}
            </AppButton>
            <AppButton color="neutral"
                       icon="i-lucide-log-in"
                       size="lg"
                       to="/auth"
                       variant="outline">
              {{ t('landing.cta.alreadyHave') }}
            </AppButton>
          </div>
        </template>
      </UPageCard>
    </UContainer>
  </section>
</template>
