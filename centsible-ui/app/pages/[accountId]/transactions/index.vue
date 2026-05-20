<script lang="ts" setup>
import {computed, ref} from 'vue';
import TransactionList from "~/components/_organisms/transactions/transaction-list.vue";
import PageHeader from "~/components/_molecules/page/page-header.vue";
import ExportButton from "~/components/_molecules/exports/export-button.vue";
import ImportCsvModal from "~/components/_organisms/transactions/modals/import-csv-modal.vue";
import AdvancedImportModal from "~/components/_organisms/transactions/modals/advanced-import-modal.vue";
import {todayIsoDate} from "~/utils/date";
import type {TransactionsParams} from "~/models/export/export-job";
import {useBudgetAccountsStore} from "~/stores/budgetAccountsStore";

definePageMeta({
  middleware: ['auth-guard', 'account-loader']
})

const {t} = useI18n();

useHead({
  title: () => t('transactions.pageTitle'),
});

const route = useRoute();
const accountId = computed(() => String(route.params.accountId ?? ''));
const accountsStore = useBudgetAccountsStore();

const isImportModalOpen = ref(false);
const isAdvancedImportOpen = ref(false);
const reloadKey = ref(0);

function buildParams(): TransactionsParams & { kind: 'TRANSACTIONS' } {
  return {
    kind: 'TRANSACTIONS',
    accountIds: accountId.value ? [accountId.value] : [],
  };
}

async function onImported() {
  await accountsStore.updateActiveAccount();
  reloadKey.value++;
}
</script>

<template>
  <UContainer class="py-6 sm:py-10">
    <PageHeader
      :description="t('transactions.description')"
      :title="t('transactions.title')"
    >
      <template #actions>
        <div class="flex flex-wrap gap-2">
          <UButton color="neutral"
                   icon="i-lucide-upload"
                   variant="outline"
                   @click="isImportModalOpen = true">
            {{ t('transactions.importCsvButton') }}
          </UButton>
          <UButton color="neutral"
                   icon="i-lucide-file-input"
                   variant="outline"
                   @click="isAdvancedImportOpen = true">
            {{ t('transactions.advancedImportButton') }}
          </UButton>
          <ExportButton
            :default-title="t('transactions.exportDefaultTitle', {date: todayIsoDate()})"
            :params-builder="buildParams"
            :label="t('transactions.exportLabel')"
            type="TRANSACTIONS"
          />
        </div>
      </template>
    </PageHeader>
    <TransactionList :key="reloadKey"/>

    <ImportCsvModal v-if="isImportModalOpen"
                    v-model:open="isImportModalOpen"
                    @imported="onImported"/>

    <AdvancedImportModal v-if="isAdvancedImportOpen"
                         v-model:open="isAdvancedImportOpen"
                         @imported="onImported"/>
  </UContainer>
</template>
