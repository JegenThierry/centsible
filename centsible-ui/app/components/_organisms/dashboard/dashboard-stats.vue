<script lang="ts" setup>
import type {Currency} from "~/models/budget-account/currency";
import StatCard from "~/components/_molecules/dashboard/stat-card.vue";
import AppButton from "~/components/_atoms/ui/app-button.vue";
import {useDashboardPeriod} from "~/composables/use-dashboard-period";
import {useMonthlyAggregates} from "~/composables/use-monthly-aggregates";

const props = defineProps<{
  accountId: string,
  currency: Currency
}>();

const {t} = useI18n();
const {window, period} = useDashboardPeriod();

const {data: aggregates, error, reload} = useMonthlyAggregates(
  () => props.accountId,
  () => window.value.months,
);

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
  <UCard v-if="error" :ui="{body: 'flex flex-col items-center justify-center gap-3 py-8 text-center'}">
    <UIcon class="w-7 h-7 text-error" name="i-lucide-triangle-alert"/>
    <p class="text-sm text-muted">{{ t('common.states.error') }}</p>
    <AppButton color="neutral" variant="soft" size="sm" icon="i-lucide-refresh-cw" @click="reload">
      {{ t('common.actions.retry') }}
    </AppButton>
  </UCard>

  <div v-else class="grid grid-cols-1 sm:grid-cols-3 gap-4 sm:gap-6">
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
