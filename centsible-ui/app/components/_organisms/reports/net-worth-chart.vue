<script lang="ts" setup>
import type {NetWorthPoint} from "~/models/reports/net-worth-point";
import type {Currency} from "~/models/budget-account/currency";
import BalanceLineChart from "~/components/_molecules/charts/balance-line-chart.vue";

const props = defineProps<{
  points: NetWorthPoint[],
  currency: Currency
}>();

const {t} = useI18n();
</script>

<template>
  <UCard>
    <template #header>
      <div class="flex items-center justify-between">
        <h3 class="text-base font-semibold text-highlighted">
          {{ t('reports.netWorth.title') }}
        </h3>
      </div>
    </template>

    <BalanceLineChart
      :currency="currency"
      :legend-label="t('reports.netWorth.legend')"
      :points="props.points"
      :tension="0.3"
      color="#ee387e"
      fill-color="rgba(238, 56, 126, 0.12)"
      height-class="h-72"
    />

    <div v-if="props.points.length === 0" class="text-sm text-muted py-4 text-center">
      {{ t('reports.netWorth.empty') }}
    </div>
  </UCard>
</template>
