<script lang="ts" setup>
const {t} = useI18n();
</script>

<template>
  <section class="relative overflow-hidden">
    <div aria-hidden="true"
         class="pointer-events-none absolute inset-x-0 -top-32 h-[28rem] bg-gradient-to-b from-primary/15 via-primary/5 to-transparent blur-3xl"/>

    <UContainer class="relative py-16 sm:py-24 md:py-32">
      <div class="flex flex-col items-center text-center gap-6 max-w-3xl mx-auto">
        <UBadge color="primary" size="lg" variant="subtle">
          <UIcon class="w-4 h-4" name="i-lucide-sparkles"/>
          {{ t('landing.hero.badge') }}
        </UBadge>

        <h1 class="text-4xl sm:text-5xl md:text-6xl font-bold tracking-tight text-highlighted">
          {{ t('landing.hero.title') }}
          <span class="text-primary">{{ t('landing.hero.titleHighlight') }}</span>
        </h1>

        <p class="text-base sm:text-lg md:text-xl text-muted leading-relaxed max-w-2xl">
          {{ t('landing.hero.subtitle') }}
        </p>

        <div class="flex flex-col sm:flex-row gap-3 mt-2">
          <UButton color="primary"
                   icon="i-lucide-rocket"
                   size="xl"
                   to="/auth"
                   trailing-icon="i-lucide-arrow-right">
            {{ t('landing.hero.getStarted') }}
          </UButton>
          <UButton color="neutral"
                   icon="i-lucide-info"
                   size="xl"
                   to="/about"
                   variant="outline">
            {{ t('landing.hero.learnMore') }}
          </UButton>
        </div>

        <p class="text-xs text-muted mt-2">
          {{ t('landing.hero.tagline') }}
        </p>
      </div>
    </UContainer>
  </section>
</template>
