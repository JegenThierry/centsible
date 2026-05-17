<script lang="ts" setup>
import AboutSectionHeader from "~/components/_molecules/about/about-section-header.vue";
import AboutTechBadge from "~/components/_molecules/about/about-tech-badge.vue";

const {t} = useI18n();

const techStack: Array<{label: string; icon: string}> = [
  {label: 'Java 21', icon: 'i-simple-icons-openjdk'},
  {label: 'Kotlin 2.2', icon: 'i-simple-icons-kotlin'},
  {label: 'Spring Boot 4', icon: 'i-simple-icons-springboot'},
  {label: 'jOOQ', icon: 'i-lucide-database'},
  {label: 'PostgreSQL 18', icon: 'i-simple-icons-postgresql'},
  {label: 'Nuxt 4', icon: 'i-simple-icons-nuxt'},
  {label: 'Vue 3', icon: 'i-simple-icons-vuedotjs'},
  {label: 'TypeScript', icon: 'i-simple-icons-typescript'},
  {label: 'Nuxt UI', icon: 'i-lucide-palette'},
  {label: 'Tailwind CSS', icon: 'i-simple-icons-tailwindcss'},
  {label: 'Pinia', icon: 'i-simple-icons-pinia'},
  {label: 'Chart.js', icon: 'i-simple-icons-chartdotjs'},
  {label: 'Docker', icon: 'i-simple-icons-docker'},
];
</script>

<template>
  <UCard>
    <template #header>
      <AboutSectionHeader icon="i-lucide-layers" :title="t('landing.about.sections.techStack')"/>
    </template>

    <div class="flex flex-wrap gap-2">
      <AboutTechBadge v-for="item in techStack"
                      :key="item.label"
                      :icon="item.icon"
                      :label="item.label"/>
    </div>
  </UCard>
</template>
