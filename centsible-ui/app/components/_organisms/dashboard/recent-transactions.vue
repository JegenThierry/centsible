<script lang="ts" setup>
import {type Transaction} from "~/models/transactions/transaction";
import type {Currency} from "~/models/budget-account/currency";
import RecentTransactionItem from "~/components/_molecules/dashboard/recent-transaction-item.vue";

const props = defineProps<{
  transactions: Transaction[],
  currency: Currency
}>();

const route = useRoute();
const {t} = useI18n();
const accountId = computed(() => route.params.accountId as string);

const recentTransactions = computed(() =>
  [...props.transactions]
    .sort((a, b) => new Date(b.transactionDate).getTime() - new Date(a.transactionDate).getTime())
    .slice(0, 5),
);
</script>

<template>
  <UCard>
    <template #header>
      <div class="flex items-center justify-between">
        <h3 class="text-base font-semibold text-highlighted">
          {{ t('accounts.dashboard.recentTransactions') }}
        </h3>
        <UButton :to="`/${accountId}/transactions`" color="neutral" size="xs" variant="ghost">
          {{ t('accounts.dashboard.viewAll') }}
        </UButton>
      </div>
    </template>

    <div class="space-y-4">
      <RecentTransactionItem
        v-for="transaction in recentTransactions"
        :key="transaction.id"
        :currency="currency"
        :transaction="transaction"
      />
      <div v-if="recentTransactions.length === 0" class="text-center py-4 text-sm text-neutral-500">
        {{ t('accounts.dashboard.noRecentTransactions') }}
      </div>
    </div>
  </UCard>
</template>
