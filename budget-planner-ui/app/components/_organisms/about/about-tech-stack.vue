<script lang="ts" setup>
import AboutSectionHeader from "~/components/_molecules/about/about-section-header.vue";
import AboutTechBadge from "~/components/_molecules/about/about-tech-badge.vue";

const techStack: Array<{label: string; icon: string}> = [
  {label: 'Kotlin', icon: 'i-simple-icons-kotlin'},
  {label: 'Spring Boot', icon: 'i-simple-icons-springboot'},
  {label: 'jOOQ', icon: 'i-lucide-database'},
  {label: 'PostgreSQL', icon: 'i-simple-icons-postgresql'},
  {label: 'Nuxt', icon: 'i-simple-icons-nuxt'},
  {label: 'Vue', icon: 'i-simple-icons-vuedotjs'},
  {label: 'Nuxt UI', icon: 'i-lucide-palette'},
  {label: 'Docker', icon: 'i-simple-icons-docker'},
];
</script>

<template>
  <UCard>
    <template #header>
      <AboutSectionHeader icon="i-lucide-layers" title="Built with"/>
    </template>

    <div class="flex flex-wrap gap-2">
      <AboutTechBadge v-for="item in techStack"
                      :key="item.label"
                      :icon="item.icon"
                      :label="item.label"/>
    </div>
  </UCard>
</template>
