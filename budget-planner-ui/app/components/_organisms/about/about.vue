<script lang="ts" setup>
import PageHeader from "~/components/_molecules/page/page-header.vue";
import AppEmptyState from "~/components/_molecules/feedback/app-empty-state.vue";
import AboutSummary from "~/components/_organisms/about/about-summary.vue";
import AboutReleaseDetails from "~/components/_organisms/about/about-release-details.vue";
import AboutTechStack from "~/components/_organisms/about/about-tech-stack.vue";
import AboutResources from "~/components/_organisms/about/about-resources.vue";
import {useSystemInformationStore} from "~/stores/systemInformationStore";

const systemInformationStore = useSystemInformationStore();
const info = computed(() => systemInformationStore.systemInformation);

onMounted(async () => {
  if (!info.value) {
    await systemInformationStore.fetchSystemInformation();
  }
});
</script>

<template>
  <UContainer class="py-6 sm:py-10">
    <PageHeader description="Version and build details about this instance of Budget Planner."
                title="About"/>

    <div v-if="systemInformationStore.pending && !info" class="flex justify-center py-12">
      <UIcon class="w-8 h-8 animate-spin text-primary" name="i-lucide-loader-circle"/>
    </div>

    <template v-else-if="info">
      <AboutSummary :info="info"/>

      <div class="grid grid-cols-1 lg:grid-cols-2 gap-4 sm:gap-6">
        <AboutReleaseDetails :info="info"/>
        <AboutTechStack/>
        <AboutResources v-if="info.repository" class="lg:col-span-2" :repository="info.repository"/>
      </div>

      <p class="mt-8 text-center text-xs text-muted">© {{ new Date().getFullYear() }} {{ info.name }}</p>
    </template>

    <AppEmptyState v-else
                   description="System information is currently unavailable."
                   icon="i-lucide-circle-alert"
                   title="Nothing to show">
      <template #actions>
        <UButton color="neutral"
                 icon="i-lucide-refresh-cw"
                 variant="outline"
                 @click="systemInformationStore.fetchSystemInformation()">
          Try again
        </UButton>
      </template>
    </AppEmptyState>
  </UContainer>
</template>
