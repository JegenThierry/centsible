<script lang="ts" setup>
import {isSameMonth, parseISO} from 'date-fns';
import {type Transaction} from "~/models/transactions/transaction";
import {CategoryType} from "~/models/category/category";
import type {Currency} from "~/models/budget-account/currency";
import StatCard from "~/components/_molecules/dashboard/stat-card.vue";

const props = defineProps<{
  transactions: Transaction[],
  currency: Currency
}>();

const {t} = useI18n();
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
      :amount="monthlyIncome"
      :currency="currency"
      color="success"
      icon="i-lucide-trending-up"
      :label="t('accounts.dashboard.monthlyIncome')"
      tone="positive"
    />

    <StatCard
      :amount="monthlyExpenses"
      :currency="currency"
      color="error"
      icon="i-lucide-trending-down"
      :label="t('accounts.dashboard.monthlyExpenses')"
      tone="negative"
    />

    <StatCard
      :amount="netSavings"
      :currency="currency"
      color="primary"
      icon="i-lucide-piggy-bank"
      :label="t('accounts.dashboard.netSavings')"
      tone="signed"
    />
  </div>
</template>
