<script lang="ts" setup>
import LandingSectionHeading from "~/components/_molecules/landing/landing-section-heading.vue";
import LandingFeatureCard from "~/components/_molecules/landing/landing-feature-card.vue";
import type {FeatureIconColor} from "~/components/_atoms/landing/landing-feature-icon.vue";

const {t} = useI18n();

const features = computed<Array<{
  icon: string;
  title: string;
  description: string;
  color: FeatureIconColor;
}>>(() => [
  {
    icon: 'i-lucide-wallet',
    title: t('landing.features.accounts.title'),
    description: t('landing.features.accounts.description'),
    color: 'primary',
  },
  {
    icon: 'i-lucide-tags',
    title: t('landing.features.categories.title'),
    description: t('landing.features.categories.description'),
    color: 'info',
  },
  {
    icon: 'i-lucide-target',
    title: t('landing.features.budgets.title'),
    description: t('landing.features.budgets.description'),
    color: 'success',
  },
  {
    icon: 'i-lucide-repeat',
    title: t('landing.features.recurring.title'),
    description: t('landing.features.recurring.description'),
    color: 'warning',
  },
  {
    icon: 'i-lucide-file-up',
    title: t('landing.features.csv.title'),
    description: t('landing.features.csv.description'),
    color: 'primary',
  },
  {
    icon: 'i-lucide-bar-chart-3',
    title: t('landing.features.dashboards.title'),
    description: t('landing.features.dashboards.description'),
    color: 'info',
  },
]);
</script>

<template>
  <section class="py-16 sm:py-24">
    <UContainer>
      <LandingSectionHeading
        align="center"
        :eyebrow="t('landing.features.eyebrow')"
        :subtitle="t('landing.features.subtitle')"
        :title="t('landing.features.title')"/>

      <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4 sm:gap-6 mt-10 sm:mt-14">
        <LandingFeatureCard
          v-for="feature in features"
          :key="feature.title"
          :color="feature.color"
          :description="feature.description"
          :icon="feature.icon"
          :title="feature.title"/>
      </div>
    </UContainer>
  </section>
</template>
