<script setup lang="ts">
import {type Transaction} from "~/models/transactions/transaction";
import type {Currency} from "~/models/budget-account/currency";
import RecentTransactionItem from "~/components/_molecules/dashboard/recent-transaction-item.vue";

const props = defineProps<{
  transactions: Transaction[],
  currency: Currency
}>();

const route = useRoute();
const accountId = computed(() => route.params.accountId as string);

const recentTransactions = computed(() => {
  if (!props.transactions) return [];
  return [...props.transactions]
    .sort((a, b) => new Date(b.transactionDate).getTime() - new Date(a.transactionDate).getTime())
    .slice(0, 5);
});
</script>

<template>
  <UCard>
    <template #header>
      <div class="flex items-center justify-between">
        <h3 class="text-base font-semibold text-gray-900 dark:text-white">
          Recent Transactions
        </h3>
        <UButton :to="`/${accountId}/transactions`" variant="ghost" color="neutral" size="xs">
          View All
        </UButton>
      </div>
    </template>

    <div class="space-y-4">
      <RecentTransactionItem
        v-for="transaction in recentTransactions"
        :key="transaction.id"
        :transaction="transaction"
        :currency="currency"
      />
      <div v-if="recentTransactions.length === 0" class="text-center py-4 text-sm text-neutral-500">
        No recent transactions
      </div>
    </div>
  </UCard>
</template>
