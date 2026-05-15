<script lang="ts" setup>
import {ArcElement, Chart as ChartJS, type ChartData, type ChartOptions, Legend, Tooltip} from 'chart.js';
import {Doughnut} from 'vue-chartjs';
import {useTransactionService} from "~/services/transactions/transaction-service";
import type {CategoryAggregate} from "~/models/transactions/transaction";
import type {Currency} from "~/models/budget-account/currency";
import BalanceNumberFormat from "~/components/_molecules/labels/balance-number-format.vue";

ChartJS.register(ArcElement, Tooltip, Legend);

const props = defineProps<{
  accountId: string;
  currency: Currency;
}>();

const colorMode = useColorMode();
const service = useTransactionService(useApi());

const aggregates = ref<CategoryAggregate[]>([]);
const loading = ref(false);

async function load() {
  if (!props.accountId) return;
  loading.value = true;
  try {
    aggregates.value = await service.aggregateByCategory(props.accountId);
  } catch (error) {
    console.error('Failed to load category aggregates', error);
    aggregates.value = [];
  } finally {
    loading.value = false;
  }
}

watch(() => props.accountId, load, {immediate: true});

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
  const isDark = colorMode.value === 'dark';
  const labelColor = isDark ? '#a3a3a3' : '#737373';

  return {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        position: 'bottom',
        labels: {color: labelColor, padding: 20, usePointStyle: true, font: {size: 11}},
      },
      tooltip: {
        callbacks: {
          label: (context) => new Intl.NumberFormat('de-DE', {
            style: 'currency',
            currency: props.currency,
          }).format(context.parsed),
        },
      },
    },
    cutout: '70%',
  };
});

const total = computed(() =>
  aggregates.value.reduce((sum, a) => sum + Math.abs(a.total), 0)
);
</script>

<template>
  <UCard>
    <template #header>
      <h3 class="text-base font-semibold text-gray-900 dark:text-white">
        Spending by category (this month)
      </h3>
    </template>

    <div class="relative h-64">
      <div v-if="aggregates.length === 0 && !loading"
           class="absolute inset-0 flex items-center justify-center text-sm text-neutral-500">
        No expenses yet this month.
      </div>
      <template v-else>
        <Doughnut :data="chartData" :options="chartOptions"/>
        <div class="absolute inset-0 flex flex-col items-center justify-center pointer-events-none mb-10">
          <span class="text-xs text-neutral-500 dark:text-neutral-400 uppercase tracking-widest font-medium">Total</span>
          <span class="text-lg font-bold text-gray-900 dark:text-white">
            <BalanceNumberFormat :balance="total" :currency="currency"/>
          </span>
        </div>
      </template>
    </div>
  </UCard>
</template>
