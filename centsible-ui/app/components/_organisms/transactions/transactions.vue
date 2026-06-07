<script lang="ts" setup>
import {computed, ref} from 'vue';
import TransactionList from "~/components/_organisms/transactions/transaction-list.vue";
import PageHeader from "~/components/_molecules/page/page-header.vue";
import ExportButton from "~/components/_molecules/exports/export-button.vue";
import AppButton from "~/components/_atoms/ui/app-button.vue";
const ImportModal = defineAsyncComponent(() => import("~/components/_organisms/transactions/modals/advanced-import-modal.vue"));
import {todayIsoDate} from "~/utils/date";
import type {TransactionsParams} from "~/models/export/export-job";
import {useBudgetAccountsStore} from "~/stores/budgetAccountsStore";

const {t} = useI18n();

const route = useRoute();
const accountId = computed(() => String(route.params.accountId ?? ''));
const accountsStore = useBudgetAccountsStore();

const isImportModalOpen = ref(false);
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
          <AppButton color="neutral"
                     icon="i-lucide-upload"
                     variant="outline"
                     @click="isImportModalOpen = true">
            {{ t('transactions.importButton') }}
          </AppButton>
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

    <ImportModal v-if="isImportModalOpen"
                 v-model:open="isImportModalOpen"
                 @imported="onImported"/>
  </UContainer>
</template>
