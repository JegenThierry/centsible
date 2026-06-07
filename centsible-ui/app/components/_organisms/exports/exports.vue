<script lang="ts" setup>
import {onMounted} from 'vue';
import {useExports} from "~/composables/use-exports";
import ExportsTable from "~/components/_organisms/exports/exports-table.vue";
import PageHeader from "~/components/_molecules/page/page-header.vue";
import RefreshButton from "~/components/_molecules/buttons/refresh-button.vue";
const ConfirmationModal = defineAsyncComponent(() => import("~/components/_organisms/modals/confirmation-modal.vue"));

const {t} = useI18n();

const pendingDeleteId = ref<string | null>(null);
const isDeleteOpen = ref(false);

function askDelete(jobId: string) {
  pendingDeleteId.value = jobId;
  isDeleteOpen.value = true;
}

async function confirmDelete() {
  if (pendingDeleteId.value) await remove(pendingDeleteId.value);
}

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
      :description="t('exports.page.description')"
      :title="t('exports.page.title')"
    >
      <template #actions>
        <RefreshButton @refresh="refresh"/>
      </template>
    </PageHeader>

    <UAlert v-if="error" class="mb-4" color="error" :description="error" icon="i-lucide-alert-triangle"/>

    <ExportsTable
      :exports="exports"
      :loading="loading"
      @delete="askDelete"
      @download="download"
      @retrigger="retrigger"
    />

    <ConfirmationModal v-if="isDeleteOpen"
                       v-model:open="isDeleteOpen"
                       :title="t('common.confirmDelete.title')"
                       :body="t('exports.delete.confirmBody')"
                       :confirm-label="t('common.actions.delete')"
                       :delete-callback="confirmDelete"
                       :manage-toasts="false"/>
  </UContainer>
</template>
