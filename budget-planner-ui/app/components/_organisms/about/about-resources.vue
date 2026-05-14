<script lang="ts" setup>
import AboutSectionHeader from "~/components/_molecules/about/about-section-header.vue";
import AboutResourceLink from "~/components/_molecules/about/about-resource-link.vue";

const props = defineProps<{
  repository: string;
}>();

const resources = computed(() => [
  {
    label: 'Source repository',
    description: 'Browse the code, open issues, or contribute.',
    icon: 'i-simple-icons-codeberg',
    to: props.repository,
  },
  {
    label: 'Report a bug',
    description: 'Found something off? Let us know on the issue tracker.',
    icon: 'i-lucide-bug',
    to: `${props.repository}/issues`,
  },
  {
    label: 'Activity',
    description: 'See what has changed recently.',
    icon: 'i-lucide-activity',
    to: `${props.repository}/activity`,
  },
]);
</script>

<template>
  <UCard>
    <template #header>
      <AboutSectionHeader icon="i-lucide-link" title="Resources"/>
    </template>

    <ul class="grid grid-cols-1 sm:grid-cols-3 gap-3">
      <li v-for="item in resources" :key="item.label">
        <AboutResourceLink :description="item.description"
                           :icon="item.icon"
                           :label="item.label"
                           :to="item.to"/>
      </li>
    </ul>
  </UCard>
</template>
