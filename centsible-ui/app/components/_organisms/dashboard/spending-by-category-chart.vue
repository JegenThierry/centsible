<script lang="ts" setup>
import {ArcElement, Chart as ChartJS, type ChartData, type ChartOptions, Legend, Tooltip} from 'chart.js';
import {Doughnut} from 'vue-chartjs';
import {useTransactionService} from "~/services/transactions/transaction-service";
import type {CategoryAggregate} from "~/models/transactions/transaction";
import type {Currency} from "~/models/budget-account/currency";
import type {CategoryDrillPayload} from "~/models/transactions/transaction-filters";
import BalanceNumberFormat from "~/components/_atoms/labels/balance-number-format.vue";
import {useDashboardPeriod} from "~/composables/use-dashboard-period";
import {useChartTheme} from "~/composables/use-chart-theme";

ChartJS.register(ArcElement, Tooltip, Legend);

const props = defineProps<{
  accountId: string;
  currency: Currency;
}>();

const emit = defineEmits<{
  'slice-click': [payload: CategoryDrillPayload];
}>();

const service = useTransactionService(useApi());
const {t} = useI18n();
const {tickColor, currencyFmt, pointerCursorOnHover} = useChartTheme(() => props.currency);
const {window} = useDashboardPeriod();

const aggregates = ref<CategoryAggregate[]>([]);
const loading = ref(false);

async function load() {
  if (!props.accountId) return;
  loading.value = true;
  try {
    const {fromIso, toIso} = window.value;
    aggregates.value = await service.aggregateByCategory(props.accountId, {
      fromDate: fromIso ?? undefined,
      toDate: toIso ?? undefined,
    });
  } catch (error) {
    console.error('Failed to load category aggregates', error);
    aggregates.value = [];
  } finally {
    loading.value = false;
  }
}

watch(() => [props.accountId, window.value.fromIso, window.value.toIso], load, {immediate: true});

const chartData = computed<ChartData<'doughnut'>>(() => ({
  labels: aggregates.value.map(a => a.categoryName),
  datasets: [{
    data: aggregates.value.map(a => Math.abs(a.total)),
    backgroundColor: aggregates.value.map(a => a.categoryColor || '#a3a3a3'),
    borderWidth: 0,
    hoverOffset: 4,
  }],
}));

const chartOptions = computed<ChartOptions<'doughnut'>>(() => {
  const fmt = currencyFmt(2);
  return {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        position: 'bottom',
        labels: {color: tickColor.value, padding: 20, usePointStyle: true, font: {size: 11}},
      },
      tooltip: {
        callbacks: {
          label: (context) => fmt.format(context.parsed),
        },
      },
    },
    cutout: '70%',
    onClick: (_evt, elements) => {
      const idx = elements?.[0]?.index;
      if (idx == null) return;
      const agg = aggregates.value[idx];
      if (!agg) return;
      emit('slice-click', {
        categoryId: agg.categoryId,
        categoryName: agg.categoryName,
        fromDate: window.value.fromIso ?? null,
        toDate: window.value.toIso ?? null,
      });
    },
    onHover: pointerCursorOnHover,
  };
});

const total = computed(() =>
  aggregates.value.reduce((sum, a) => sum + Math.abs(a.total), 0)
);
</script>

<template>
  <UCard>
    <template #header>
      <h3 class="text-base font-semibold text-highlighted">
        {{ t('accounts.dashboard.spendingByCategory') }}
      </h3>
    </template>

    <div class="relative h-64">
      <div v-if="aggregates.length === 0 && !loading"
           class="absolute inset-0 flex items-center justify-center text-sm text-neutral-500">
        {{ t('accounts.dashboard.noExpenses') }}
      </div>
      <template v-else>
        <Doughnut :data="chartData" :options="chartOptions"/>
        <div class="absolute inset-0 flex flex-col items-center justify-center pointer-events-none mb-10">
          <span class="text-xs text-muted uppercase tracking-widest font-medium">{{ t('accounts.dashboard.total') }}</span>
          <span class="text-lg font-bold text-highlighted">
            <BalanceNumberFormat :balance="total" :currency="currency"/>
          </span>
        </div>
      </template>
    </div>
  </UCard>
</template>
