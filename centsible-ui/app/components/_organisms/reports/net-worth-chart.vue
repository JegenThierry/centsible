<script lang="ts" setup>
import type {NetWorthPoint} from "~/models/reports/net-worth-point";
import type {Currency} from "~/models/budget-account/currency";
import BalanceLineChart from "~/components/_molecules/charts/balance-line-chart.vue";
import {useChartTheme} from "~/composables/use-chart-theme";

const props = defineProps<{
  points: NetWorthPoint[],
  currency: Currency
}>();

const emit = defineEmits<{
  'point-click': [date: string];
}>();

const {t} = useI18n();
const {primaryColor, withAlpha} = useChartTheme(() => props.currency);
const lineColor = computed(() => primaryColor.value);
const lineFillColor = computed(() => withAlpha(primaryColor.value, 0.12));
</script>

<template>
  <UCard>
    <template #header>
      <div class="flex items-center justify-between">
        <h3 class="text-base font-semibold text-highlighted">
          {{ t('reports.netWorth.title') }}
        </h3>
        <p class="text-xs text-muted">{{ t('reports.netWorth.drillHint') }}</p>
      </div>
    </template>

    <BalanceLineChart
      :currency="currency"
      :legend-label="t('reports.netWorth.legend')"
      :points="props.points"
      :color="lineColor"
      :fill-color="lineFillColor"
      height-class="h-72"
      @point-click="(d: string) => emit('point-click', d)"
    />

    <div v-if="props.points.length === 0" class="text-sm text-muted py-4 text-center">
      {{ t('reports.netWorth.empty') }}
    </div>
  </UCard>
</template>
