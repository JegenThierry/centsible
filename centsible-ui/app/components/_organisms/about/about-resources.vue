<script lang="ts" setup>
import AboutSectionHeader from "~/components/_molecules/about/about-section-header.vue";
import AboutResourceLink from "~/components/_molecules/about/about-resource-link.vue";
import {REPOSITORY_URL} from "~/models/constants";

const {t} = useI18n();

const resources = computed(() => [
  {
    label: t('landing.about.resources.sourceLabel'),
    description: t('landing.about.resources.sourceDescription'),
    icon: 'i-simple-icons-github',
    to: REPOSITORY_URL,
  },
  {
    label: t('landing.about.resources.bugLabel'),
    description: t('landing.about.resources.bugDescription'),
    icon: 'i-lucide-bug',
    to: `${REPOSITORY_URL}/issues`,
  },
  {
    label: t('landing.about.resources.activityLabel'),
    description: t('landing.about.resources.activityDescription'),
    icon: 'i-lucide-activity',
    to: `${REPOSITORY_URL}/activity`,
  },
  {
    label: t('landing.about.resources.donateLabel'),
    description: t('landing.about.resources.donateDescription'),
    icon: 'i-simple-icons-kofi',
    to: 'https://ko-fi.com/thierryjegen',
  },
]);
</script>

<template>
  <UCard>
    <template #header>
      <AboutSectionHeader icon="i-lucide-link" :title="t('landing.about.sections.resources')"/>
    </template>

    <ul class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3">
      <li v-for="item in resources" :key="item.label">
        <AboutResourceLink :description="item.description"
                           :icon="item.icon"
                           :label="item.label"
                           :to="item.to"/>
      </li>
    </ul>
  </UCard>
</template>
