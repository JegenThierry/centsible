<script lang="ts" setup>
</script>

<template>
  <section class="relative overflow-hidden">
    <div aria-hidden="true"
         class="pointer-events-none absolute inset-x-0 -top-32 h-[28rem] bg-gradient-to-b from-primary/15 via-primary/5 to-transparent blur-3xl"/>

    <UContainer class="relative py-16 sm:py-24 md:py-32">
      <div class="flex flex-col items-center text-center gap-6 max-w-3xl mx-auto">
        <UBadge color="primary" size="lg" variant="subtle">
          <UIcon class="w-4 h-4" name="i-lucide-sparkles"/>
          Open source · Self-hosted
        </UBadge>

        <h1 class="text-4xl sm:text-5xl md:text-6xl font-bold tracking-tight text-highlighted">
          Take control of your money,
          <span class="text-primary">without the bloat.</span>
        </h1>

        <p class="text-base sm:text-lg md:text-xl text-muted leading-relaxed max-w-2xl">
          A clean, self-hosted budget tracker with multiple accounts, smart categories,
          monthly budgets, and dashboards that actually help you decide.
        </p>

        <div class="flex flex-col sm:flex-row gap-3 mt-2">
          <UButton color="primary"
                   icon="i-lucide-rocket"
                   size="xl"
                   to="/auth"
                   trailing-icon="i-lucide-arrow-right">
            Get started
          </UButton>
          <UButton color="neutral"
                   icon="i-lucide-info"
                   size="xl"
                   to="/about"
                   variant="outline">
            Learn more
          </UButton>
        </div>

        <p class="text-xs text-muted mt-2">
          Your data lives on your server. No tracking, no ads, no nonsense.
        </p>
      </div>
    </UContainer>
  </section>
</template>
