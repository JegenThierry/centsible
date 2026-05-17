<script lang="ts" setup>
import {type Transaction} from "~/models/transactions/transaction";
import type {Currency} from "~/models/budget-account/currency";
import TransactionAmount from "~/components/_molecules/transactions/transaction-amount.vue";
import CategoryIcon from "~/components/_atoms/categories/category-icon.vue";
import FormattedDate from "~/components/_atoms/labels/formatted-date.vue";

defineProps<{
  transaction: Transaction;
  currency: Currency;
}>();

const {t} = useI18n();
</script>

<template>
  <div class="flex items-center justify-between">
    <div class="flex items-center gap-3 overflow-hidden">
      <CategoryIcon :color="transaction.category?.color" :icon="transaction.category?.icon" size="md"/>
      <div class="flex flex-col overflow-hidden">
        <span class="text-sm font-medium text-gray-900 dark:text-white truncate">
          {{ transaction.description || t('transactions.noDescription') }}
        </span>
        <span class="text-xs text-neutral-500 dark:text-neutral-400 truncate">
          {{ transaction.category?.name || t('transactions.uncategorized') }}
        </span>
      </div>
    </div>
    <div class="text-right shrink-0 ml-2">
      <TransactionAmount :amount="transaction.amount" :currency="currency" :type="transaction.category?.type"/>
      <div class="text-[10px] text-neutral-400 dark:text-neutral-500 uppercase">
        <FormattedDate :date="transaction.transactionDate" format="short"/>
      </div>
    </div>
  </div>
</template>
