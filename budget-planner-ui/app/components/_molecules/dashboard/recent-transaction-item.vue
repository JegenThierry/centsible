<script setup lang="ts">
import {type Transaction} from "~/models/transactions/transaction";
import type {Currency} from "~/models/budget-account/currency";
import TransactionAmount from "~/components/_molecules/transactions/transaction-amount.vue";

defineProps<{
  transaction: Transaction;
  currency: Currency;
}>();
</script>

<template>
  <div class="flex items-center justify-between">
    <div class="flex items-center gap-3 overflow-hidden">
      <div class="p-2 rounded-lg shrink-0"
           :style="{ backgroundColor: `${transaction.category?.color || '#a3a3a3'}15`, color: transaction.category?.color || '#a3a3a3' }">
        <UIcon :name="transaction.category?.icon || 'i-lucide-circle-help'" class="w-5 h-5"/>
      </div>
      <div class="flex flex-col overflow-hidden">
        <span class="text-sm font-medium text-gray-900 dark:text-white truncate">
          {{ transaction.description || 'No description' }}
        </span>
        <span class="text-xs text-neutral-500 dark:text-neutral-400 truncate">
          {{ transaction.category?.name || 'Uncategorized' }}
        </span>
      </div>
    </div>
    <div class="text-right shrink-0 ml-2">
      <TransactionAmount :amount="transaction.amount" :type="transaction.category?.type" :currency="currency" />
      <div class="text-[10px] text-neutral-400 dark:text-neutral-500 uppercase">
        {{ new Date(transaction.transactionDate).toLocaleDateString('de-DE', {day: '2-digit', month: '2-digit'}) }}
      </div>
    </div>
  </div>
</template>
