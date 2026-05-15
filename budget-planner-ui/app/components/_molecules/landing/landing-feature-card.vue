<script lang="ts" setup>
import LandingFeatureIcon from "~/components/_atoms/landing/landing-feature-icon.vue";

defineProps<{
  icon: string;
  title: string;
  description: string;
  colorClass?: string;
}>();
</script>

<template>
  <UCard
    class="h-full transition duration-200 hover:-translate-y-0.5 hover:shadow-md"
    variant="soft">
    <div class="flex flex-col gap-3">
      <LandingFeatureIcon :color-class="colorClass" :icon="icon"/>
      <h3 class="text-base sm:text-lg font-semibold text-highlighted">{{ title }}</h3>
      <p class="text-sm text-muted leading-relaxed">{{ description }}</p>
    </div>
  </UCard>
</template>
