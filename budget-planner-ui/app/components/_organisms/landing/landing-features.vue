<script lang="ts" setup>
import LandingSectionHeading from "~/components/_molecules/landing/landing-section-heading.vue";
import LandingFeatureCard from "~/components/_molecules/landing/landing-feature-card.vue";

const features: Array<{
  icon: string;
  title: string;
  description: string;
  colorClass: string;
}> = [
  {
    icon: 'i-lucide-wallet',
    title: 'Multiple accounts',
    description: 'Track checking, savings, and cards side by side — each with their own currency and running balance.',
    colorClass: 'bg-blue-100 dark:bg-blue-900/30 text-blue-600 dark:text-blue-400',
  },
  {
    icon: 'i-lucide-tags',
    title: 'Smart categories',
    description: 'Tag income and expenses to see exactly where your money comes from and where it goes.',
    colorClass: 'bg-violet-100 dark:bg-violet-900/30 text-violet-600 dark:text-violet-400',
  },
  {
    icon: 'i-lucide-target',
    title: 'Monthly budgets',
    description: 'Set per-category spending goals and watch overspend surface before the month is over.',
    colorClass: 'bg-emerald-100 dark:bg-emerald-900/30 text-emerald-600 dark:text-emerald-400',
  },
  {
    icon: 'i-lucide-repeat',
    title: 'Recurring transactions',
    description: 'Automate the predictable: rent, salary, subscriptions — log them once and forget.',
    colorClass: 'bg-amber-100 dark:bg-amber-900/30 text-amber-600 dark:text-amber-400',
  },
  {
    icon: 'i-lucide-file-up',
    title: 'CSV import',
    description: 'Pull statements straight from your bank with column mapping and built-in deduplication.',
    colorClass: 'bg-rose-100 dark:bg-rose-900/30 text-rose-600 dark:text-rose-400',
  },
  {
    icon: 'i-lucide-bar-chart-3',
    title: 'Insightful dashboards',
    description: 'Charts and stats that show trends, breakdowns, and the bottom line at a glance.',
    colorClass: 'bg-cyan-100 dark:bg-cyan-900/30 text-cyan-600 dark:text-cyan-400',
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
          :color-class="feature.colorClass"
          :description="feature.description"
          :icon="feature.icon"
          :title="feature.title"/>
      </div>
    </UContainer>
  </section>
</template>
