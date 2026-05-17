<script lang="ts" setup>
import type {Currency} from "~/models/budget-account/currency";
import type {MonthlyAggregate} from "~/models/transactions/transaction";
import StatCard from "~/components/_molecules/dashboard/stat-card.vue";
import {useDashboardPeriod} from "~/composables/use-dashboard-period";
import {useTransactionService} from "~/services/transactions/transaction-service";

const props = defineProps<{
  accountId: string,
  currency: Currency
}>();

const {t} = useI18n();
const {window, period} = useDashboardPeriod();
const service = useTransactionService(useApi());

const aggregates = ref<MonthlyAggregate[]>([]);

async function load() {
  if (!props.accountId) return;
  try {
    aggregates.value = await service.aggregateByMonth(props.accountId, window.value.months);
  } catch (error) {
    console.error('Failed to load monthly aggregates for stats', error);
    aggregates.value = [];
  }
}

watch(() => [props.accountId, window.value.months], load, {immediate: true});

const income = computed(() =>
  aggregates.value.reduce((sum, m) => sum + (Number(m.income) || 0), 0)
);

const expenses = computed(() =>
  aggregates.value.reduce((sum, m) => sum + (Number(m.expense) || 0), 0)
);

const netSavings = computed(() => income.value - expenses.value);

const incomeLabel = computed(() => t(`accounts.dashboard.stats.income.${period.value}`));
const expensesLabel = computed(() => t(`accounts.dashboard.stats.expenses.${period.value}`));
</script>

<template>
  <div class="grid grid-cols-1 sm:grid-cols-3 gap-4 sm:gap-6">
    <StatCard
      :amount="income"
      :currency="currency"
      color="success"
      icon="i-lucide-trending-up"
      :label="incomeLabel"
      tone="positive"
    />

    <StatCard
      :amount="expenses"
      :currency="currency"
      color="error"
      icon="i-lucide-trending-down"
      :label="expensesLabel"
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
