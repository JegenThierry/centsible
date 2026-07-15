<script lang="ts" setup>
import type {ChartData, ChartOptions} from 'chart.js';
import {Line} from 'vue-chartjs';
import '~/utils/chart-registry';
import type {CategorySpendingSeries} from "~/models/reports/category-spending";
import type {Currency} from "~/models/budget-account/currency";
import {useChartTheme} from "~/composables/use-chart-theme";

const props = defineProps<{
  series: CategorySpendingSeries[];
  currency: Currency;
}>();

const {tickColor, gridColor, currencyFmt, chartLegend} = useChartTheme(() => props.currency);
const {t} = useI18n();

const allMonths = computed<string[]>(() => {
  const set = new Set<string>();
  for (const s of props.series) for (const t of s.totals) set.add(t.yearMonth);
  return Array.from(set).sort();
});

const chartData = computed<ChartData<'line'>>(() => {
  const labels = allMonths.value;
  return {
    labels,
    datasets: props.series.map(s => {
      const byMonth = new Map(s.totals.map(t => [t.yearMonth, Number(t.amount)]));
      return {
        label: s.categoryName,
        data: labels.map(m => byMonth.get(m) ?? 0),
        borderColor: s.categoryColor ?? '#a3a3a3',
        backgroundColor: s.categoryColor ?? '#a3a3a3',
        tension: 0,
        pointRadius: 3,
        pointHoverRadius: 5,
      };
    }),
  };
});

const chartOptions = computed<ChartOptions<'line'>>(() => {
  const fmt = currencyFmt();
  return {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: chartLegend(),
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
      <h3 class="text-base font-semibold text-highlighted">
        {{ t('reports.categorySpending.title') }}
      </h3>
    </template>

    <div v-if="series.length === 0" class="text-sm text-muted py-4 text-center">
      {{ t('reports.categorySpending.empty') }}
    </div>
    <div v-else class="h-72">
      <Line :data="chartData" :options="chartOptions"/>
    </div>
  </UCard>
</template>
