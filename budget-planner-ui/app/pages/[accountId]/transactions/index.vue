<script lang="ts" setup>
import {computed} from 'vue';
import TransactionList from "~/components/_organisms/transactions/transaction-list.vue";
import PageHeader from "~/components/_molecules/page/page-header.vue";
import ExportButton from "~/components/_molecules/exports/export-button.vue";
import {todayIsoDate} from "~/utils/date";
import type {TransactionsParams} from "~/models/export/export-job";

definePageMeta({
  middleware: ['auth-guard', 'account-loader']
})

useHead({
  title: 'Transactions | Budget Planner',
});

const route = useRoute();
const accountId = computed(() => String(route.params.accountId ?? ''));

function buildParams(): TransactionsParams & { kind: 'TRANSACTIONS' } {
  return {
    kind: 'TRANSACTIONS',
    accountIds: accountId.value ? [accountId.value] : [],
  };
}
</script>

<template>
  <UContainer class="py-6 sm:py-10">
    <PageHeader
      description="View and manage your transactions"
      title="Transactions"
    >
      <template #actions>
        <ExportButton
          :default-title="`Transactions ${todayIsoDate()}`"
          :params-builder="buildParams"
          label="Export transactions"
          type="TRANSACTIONS"
        />
      </template>
    </PageHeader>
    <TransactionList/>
  </UContainer>
</template>
