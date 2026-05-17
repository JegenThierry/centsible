<script lang="ts" setup>
import FormattedDate from "~/components/_atoms/labels/formatted-date.vue";
import AboutSectionHeader from "~/components/_molecules/about/about-section-header.vue";
import AboutInfoRow from "~/components/_molecules/about/about-info-row.vue";
import type {SystemInformation} from "~/models/system/system-information";

defineProps<{
  info: SystemInformation;
}>();

const {t} = useI18n();
</script>

<template>
  <UCard>
    <template #header>
      <AboutSectionHeader icon="i-lucide-info" :title="t('landing.about.sections.release')"/>
    </template>

    <dl class="divide-y divide-default">
      <AboutInfoRow :label="t('landing.about.fields.version')">{{ info.version }}</AboutInfoRow>

      <AboutInfoRow v-if="info.releasedAt" :label="t('landing.about.fields.released')">
        <FormattedDate :date="info.releasedAt" format="date"/>
      </AboutInfoRow>

      <AboutInfoRow v-if="info.license" :label="t('landing.about.fields.license')">{{ info.license }}</AboutInfoRow>

      <AboutInfoRow v-if="info.repository" :label="t('landing.about.fields.repository')">
        <ULink :to="info.repository" target="_blank">{{ t('landing.about.fields.openLink') }}</ULink>
      </AboutInfoRow>
    </dl>
  </UCard>
</template>
