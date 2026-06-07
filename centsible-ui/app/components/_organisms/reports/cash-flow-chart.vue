<script lang="ts" setup>
import type {ChartData, ChartOptions} from 'chart.js';
import {Bar} from 'vue-chartjs';
import type {CashFlowPoint} from "~/models/reports/cash-flow";
import type {Currency} from "~/models/budget-account/currency";
import {useChartTheme} from "~/composables/use-chart-theme";

const props = defineProps<{
  points: CashFlowPoint[];
  currency: Currency;
}>();

const {tickColor, gridColor, currencyFmt, primaryColor, successColor, errorColor} = useChartTheme(() => props.currency);
const {t} = useI18n();

const chartData = computed<ChartData<'bar'>>(() => ({
  labels: props.points.map(p => p.yearMonth),
  datasets: [
    {
      label: t('reports.cashFlow.income'),
      data: props.points.map(p => Number(p.income)),
      backgroundColor: successColor.value,
    },
    {
      label: t('reports.cashFlow.expense'),
      data: props.points.map(p => Number(p.expense)),
      backgroundColor: errorColor.value,
    },
    {
      label: t('reports.cashFlow.net'),
      data: props.points.map(p => Number(p.net)),
      backgroundColor: primaryColor.value,
    },
  ],
}));

const chartOptions = computed<ChartOptions<'bar'>>(() => {
  const fmt = currencyFmt();
  return {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {position: 'bottom', labels: {color: tickColor.value, usePointStyle: true, font: {size: 11}}},
      tooltip: {
        callbacks: {
          label: (ctx) => `${ctx.dataset.label}: ${fmt.format(ctx.parsed.y as number)}`,
        },
      },
    },
    scales: {
      y: {
        grid: {color: gridColor.value},
        ticks: {color: tickColor.value, callback: (v) => fmt.format(v as number)},
      },
      x: {grid: {display: false}, ticks: {color: tickColor.value}},
    },
  };
});
</script>

<template>
  <UCard>
    <template #header>
      <h3 class="text-base font-semibold text-highlighted">{{ t('reports.cashFlow.title') }}</h3>
    </template>

    <div v-if="points.length === 0" class="text-sm text-muted py-4 text-center">
      {{ t('reports.cashFlow.empty') }}
    </div>
    <div v-else class="h-72">
      <Bar :data="chartData" :options="chartOptions"/>
    </div>
  </UCard>
</template>
