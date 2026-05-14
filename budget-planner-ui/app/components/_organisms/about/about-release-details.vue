<script lang="ts" setup>
import FormattedDate from "~/components/_atoms/labels/formatted-date.vue";
import AboutSectionHeader from "~/components/_molecules/about/about-section-header.vue";
import AboutInfoRow from "~/components/_molecules/about/about-info-row.vue";
import type {SystemInformation} from "~/models/system/system-information";

defineProps<{
  info: SystemInformation;
}>();
</script>

<template>
  <UCard>
    <template #header>
      <AboutSectionHeader icon="i-lucide-info" title="Release details"/>
    </template>

    <dl class="divide-y divide-default">
      <AboutInfoRow label="Version">{{ info.version }}</AboutInfoRow>

      <AboutInfoRow v-if="info.releasedAt" label="Released">
        <FormattedDate :date="info.releasedAt" format="date"/>
      </AboutInfoRow>

      <AboutInfoRow v-if="info.license" label="License">{{ info.license }}</AboutInfoRow>

      <AboutInfoRow v-if="info.repository" label="Repository">
        <ULink :to="info.repository" target="_blank">Open</ULink>
      </AboutInfoRow>
    </dl>
  </UCard>
</template>
