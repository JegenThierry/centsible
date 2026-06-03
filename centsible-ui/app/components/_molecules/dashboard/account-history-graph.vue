<script lang="ts" setup>
import type {BudgetAccountSnapshot} from "~/models/budget-account/budget-account";
import type {Currency} from "~/models/budget-account/currency";
import BalanceLineChart from "~/components/_molecules/charts/balance-line-chart.vue";

const props = defineProps<{
  snapshots: BudgetAccountSnapshot[],
  currency: Currency
}>();

const {t} = useI18n();

const points = computed(() =>
  (props.snapshots ?? []).map(s => ({date: s.createdAt, balance: s.balance}))
);
</script>

<template>
  <UCard>
    <template #header>
      <div class="flex items-center justify-between">
        <h3 class="text-base font-semibold text-highlighted">
          {{ t('accounts.dashboard.balanceOverTime') }}
        </h3>
      </div>
    </template>

    <BalanceLineChart
      :currency="currency"
      :legend-label="t('accounts.dashboard.balance')"
      :points="points"
      color="#10b981"
      fill-color="rgba(16, 185, 129, 0.1)"
    />
  </UCard>
</template>
