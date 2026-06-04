<script lang="ts" setup>
import type {BudgetAccountSnapshot} from "~/models/budget-account/budget-account";
import type {Currency} from "~/models/budget-account/currency";
import BalanceLineChart from "~/components/_molecules/charts/balance-line-chart.vue";
import {useChartTheme} from "~/composables/use-chart-theme";

const props = defineProps<{
  snapshots: BudgetAccountSnapshot[],
  currency: Currency
}>();

const {t} = useI18n();
const {primaryColor, withAlpha} = useChartTheme(() => props.currency);

const points = computed(() =>
  (props.snapshots ?? []).map(s => ({date: s.createdAt, balance: s.balance}))
);

const fillColor = computed(() => withAlpha(primaryColor.value, 0.12));
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
      :color="primaryColor"
      :currency="currency"
      :fill-color="fillColor"
      :legend-label="t('accounts.dashboard.balance')"
      :points="points"
    />
  </UCard>
</template>
