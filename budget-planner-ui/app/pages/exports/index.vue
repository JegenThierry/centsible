<script lang="ts" setup>
import {onMounted} from 'vue';
import {useExports} from "~/composables/use-exports";
import ExportsTable from "~/components/_organisms/exports/exports-table.vue";
import PageHeader from "~/components/_molecules/page/page-header.vue";
import RefreshButton from "~/components/_molecules/buttons/refresh-button.vue";

definePageMeta({
  middleware: ['auth-guard'],
});

useHead({
  title: 'My Documents | Budget Planner',
});

const {
  exports,
  loading,
  error,
  refresh,
  retrigger,
  remove,
  download,
  startPolling,
} = useExports();

onMounted(async () => {
  await refresh();
  startPolling();
});
</script>

<template>
  <UContainer class="py-6 sm:py-10">
    <PageHeader
      description="Track and download your generated exports. Re-trigger to refresh with current data."
      title="My Documents"
    >
      <template #actions>
        <RefreshButton @refresh="refresh"/>
      </template>
    </PageHeader>

    <UAlert v-if="error" class="mb-4" color="error" :description="error" icon="i-lucide-alert-triangle"/>

    <ExportsTable
      :exports="exports"
      :loading="loading"
      @delete="remove"
      @download="download"
      @retrigger="retrigger"
    />
  </UContainer>
</template>
