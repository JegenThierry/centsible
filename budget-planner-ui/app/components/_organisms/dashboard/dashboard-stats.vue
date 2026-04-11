<script setup lang="ts">
import { isSameMonth, parseISO } from 'date-fns';
import { type Transaction } from "~/models/transactions/transaction";
import { CategoryType } from "~/models/category/category";
import type { Currency } from "~/models/budget-account/currency";
import StatCard from "~/components/_molecules/dashboard/stat-card.vue";

const props = defineProps<{
  transactions: Transaction[],
  currency: Currency
}>();

const now = new Date();

const monthlyIncome = computed(() => {
  return props.transactions
    .filter(t => t.category.type === CategoryType.INCOME && isSameMonth(parseISO(t.transactionDate), now))
    .reduce((sum, t) => sum + Math.abs(t.amount), 0);
});

const monthlyExpenses = computed(() => {
  return props.transactions
    .filter(t => t.category.type === CategoryType.EXPENSE && isSameMonth(parseISO(t.transactionDate), now))
    .reduce((sum, t) => sum + Math.abs(t.amount), 0);
});

const netSavings = computed(() => {
  return monthlyIncome.value - monthlyExpenses.value;
});
</script>

<template>
  <div class="grid grid-cols-1 sm:grid-cols-3 gap-4 sm:gap-6">
    <StatCard
      label="Monthly Income"
      :amount="monthlyIncome"
      :currency="currency"
      icon="i-lucide-trending-up"
      icon-color-class="bg-green-100 dark:bg-green-900/30 text-green-600 dark:text-green-400"
      amount-color-class="text-green-600 dark:text-green-400"
    />

    <StatCard
      label="Monthly Expenses"
      :amount="monthlyExpenses"
      :currency="currency"
      icon="i-lucide-trending-down"
      icon-color-class="bg-red-100 dark:bg-red-900/30 text-red-600 dark:text-red-400"
      amount-color-class="text-red-600 dark:text-red-400"
    />

    <StatCard
      label="Net Savings"
      :amount="netSavings"
      :currency="currency"
      icon="i-lucide-piggy-bank"
      icon-color-class="bg-blue-100 dark:bg-blue-900/30 text-blue-600 dark:text-blue-400"
      :amount-color-class="netSavings >= 0 ? 'text-green-600 dark:text-green-400' : 'text-red-600 dark:text-red-400'"
    />
  </div>
</template>
