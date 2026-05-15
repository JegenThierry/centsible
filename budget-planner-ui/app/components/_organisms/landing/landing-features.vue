<script lang="ts" setup>
import LandingSectionHeading from "~/components/_molecules/landing/landing-section-heading.vue";
import LandingFeatureCard from "~/components/_molecules/landing/landing-feature-card.vue";
import type {FeatureIconColor} from "~/components/_atoms/landing/landing-feature-icon.vue";

const features: Array<{
  icon: string;
  title: string;
  description: string;
  color: FeatureIconColor;
}> = [
  {
    icon: 'i-lucide-wallet',
    title: 'Multiple accounts',
    description: 'Track checking, savings, and cards side by side — each with their own currency and running balance.',
    color: 'primary',
  },
  {
    icon: 'i-lucide-tags',
    title: 'Smart categories',
    description: 'Tag income and expenses to see exactly where your money comes from and where it goes.',
    color: 'info',
  },
  {
    icon: 'i-lucide-target',
    title: 'Monthly budgets',
    description: 'Set per-category spending goals and watch overspend surface before the month is over.',
    color: 'success',
  },
  {
    icon: 'i-lucide-repeat',
    title: 'Recurring transactions',
    description: 'Automate the predictable: rent, salary, subscriptions — log them once and forget.',
    color: 'warning',
  },
  {
    icon: 'i-lucide-file-up',
    title: 'CSV import',
    description: 'Pull statements straight from your bank with column mapping and built-in deduplication.',
    color: 'primary',
  },
  {
    icon: 'i-lucide-bar-chart-3',
    title: 'Insightful dashboards',
    description: 'Charts and stats that show trends, breakdowns, and the bottom line at a glance.',
    color: 'info',
  },
];
</script>

<template>
  <section class="py-16 sm:py-24">
    <UContainer>
      <LandingSectionHeading
        align="center"
        eyebrow="Features"
        subtitle="The essentials, done well. No upsell, no premium tier."
        title="Everything you need to budget, nothing you don't."/>

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
