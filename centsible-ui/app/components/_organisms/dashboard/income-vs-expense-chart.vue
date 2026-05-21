<script lang="ts" setup>
import type {ChartData, ChartOptions} from 'chart.js';
import {Bar} from 'vue-chartjs';
import type {Currency} from "~/models/budget-account/currency";
import {useDashboardPeriod} from "~/composables/use-dashboard-period";
import {useMonthlyAggregates} from "~/composables/use-monthly-aggregates";
import {useChartTheme} from "~/composables/use-chart-theme";

const props = defineProps<{
  accountId: string;
  currency: Currency;
  months?: number;
}>();

const {t} = useI18n();
const localeTag = useLocaleTag();
const {isDark, tickColor, currencyFmt} = useChartTheme(() => props.currency);
const {window} = useDashboardPeriod();

const resolvedMonths = computed(() => Math.max(1, Math.min(36, props.months ?? window.value.months)));

const {data: aggregates, loading} = useMonthlyAggregates(
  () => props.accountId,
  () => resolvedMonths.value,
);

function formatLabel(yearMonth: string): string {
  const [year, month] = yearMonth.split('-');
  if (!year || !month) return yearMonth;
  const date = new Date(Number(year), Number(month) - 1, 1);
  return date.toLocaleDateString(localeTag.value, {month: 'short', year: '2-digit'});
}

const chartData = computed<ChartData<'bar'>>(() => ({
  labels: aggregates.value.map(a => formatLabel(a.yearMonth)),
  datasets: [
    {
      label: t('accounts.dashboard.income'),
      data: aggregates.value.map(a => Number(a.income) || 0),
      backgroundColor: '#10b981',
      borderRadius: 4,
    },
    {
      label: t('accounts.dashboard.expense'),
      data: aggregates.value.map(a => Number(a.expense) || 0),
      backgroundColor: '#ef4444',
      borderRadius: 4,
    },
  ],
}));

const chartOptions = computed<ChartOptions<'bar'>>(() => {
  // This chart uses a softer rgba grid (existing visual choice) instead of the shared gridColor.
  const softGrid = isDark.value ? 'rgba(255,255,255,0.06)' : 'rgba(0,0,0,0.06)';
  const fmt = currencyFmt(2);

  return {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        position: 'bottom',
        labels: {color: tickColor.value, usePointStyle: true, font: {size: 11}},
      },
      tooltip: {
        callbacks: {
          label: (ctx) => `${ctx.dataset.label}: ${fmt.format(Number(ctx.parsed.y))}`,
        },
      },
    },
    scales: {
      x: {
        ticks: {color: tickColor.value, font: {size: 11}},
        grid: {display: false},
      },
      y: {
        beginAtZero: true,
        ticks: {color: tickColor.value, font: {size: 11}},
        grid: {color: softGrid},
      },
    },
  };
});

const hasData = computed(() =>
  aggregates.value.some(a => Number(a.income) > 0 || Number(a.expense) > 0)
);
</script>

<template>
  <UCard>
    <template #header>
      <h3 class="text-base font-semibold text-highlighted">
        {{ t('accounts.dashboard.incomeVsExpense') }}
      </h3>
    </template>

    <div class="relative h-64">
      <div v-if="!hasData && !loading"
           class="absolute inset-0 flex items-center justify-center text-sm text-neutral-500">
        {{ t('accounts.dashboard.noActivity') }}
      </div>
      <Bar v-else :data="chartData" :options="chartOptions"/>
    </div>
  </UCard>
</template>
