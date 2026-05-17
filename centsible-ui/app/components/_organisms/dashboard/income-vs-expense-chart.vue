<script lang="ts" setup>
import {
  BarElement,
  CategoryScale,
  Chart as ChartJS,
  type ChartData,
  type ChartOptions,
  Legend,
  LinearScale,
  Tooltip,
} from 'chart.js';
import {Bar} from 'vue-chartjs';
import {useTransactionService} from "~/services/transactions/transaction-service";
import type {MonthlyAggregate} from "~/models/transactions/transaction";
import type {Currency} from "~/models/budget-account/currency";
import {useDashboardPeriod} from "~/composables/use-dashboard-period";

ChartJS.register(BarElement, CategoryScale, LinearScale, Tooltip, Legend);

const props = defineProps<{
  accountId: string;
  currency: Currency;
  months?: number;
}>();

const colorMode = useColorMode();
const service = useTransactionService(useApi());
const {t} = useI18n();
const localeTag = useLocaleTag();
const {window} = useDashboardPeriod();

const aggregates = ref<MonthlyAggregate[]>([]);
const loading = ref(false);

const resolvedMonths = computed(() => Math.max(1, Math.min(36, props.months ?? window.value.months)));

async function load() {
  if (!props.accountId) return;
  loading.value = true;
  try {
    aggregates.value = await service.aggregateByMonth(props.accountId, resolvedMonths.value);
  } catch (error) {
    console.error('Failed to load monthly aggregates', error);
    aggregates.value = [];
  } finally {
    loading.value = false;
  }
}

watch(() => [props.accountId, resolvedMonths.value], load, {immediate: true});

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
  const isDark = colorMode.value === 'dark';
  const labelColor = isDark ? '#a3a3a3' : '#737373';
  const gridColor = isDark ? 'rgba(255,255,255,0.06)' : 'rgba(0,0,0,0.06)';
  const currencyFmt = new Intl.NumberFormat(localeTag.value, {style: 'currency', currency: props.currency});

  return {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        position: 'bottom',
        labels: {color: labelColor, usePointStyle: true, font: {size: 11}},
      },
      tooltip: {
        callbacks: {
          label: (ctx) => `${ctx.dataset.label}: ${currencyFmt.format(Number(ctx.parsed.y))}`,
        },
      },
    },
    scales: {
      x: {
        ticks: {color: labelColor, font: {size: 11}},
        grid: {display: false},
      },
      y: {
        beginAtZero: true,
        ticks: {color: labelColor, font: {size: 11}},
        grid: {color: gridColor},
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
