<script lang="ts" setup>
import {ArcElement, Chart as ChartJS, type ChartData, type ChartOptions, Legend, Tooltip} from 'chart.js';
import {Doughnut} from 'vue-chartjs';
import {useTransactionService} from "~/services/transactions/transaction-service";
import type {CategoryAggregate} from "~/models/transactions/transaction";
import type {Currency} from "~/models/budget-account/currency";
import BalanceNumberFormat from "~/components/_atoms/labels/balance-number-format.vue";

ChartJS.register(ArcElement, Tooltip, Legend);

const props = defineProps<{
  accountId: string;
  currency: Currency;
}>();

const colorMode = useColorMode();
const service = useTransactionService(useApi());
const {t} = useI18n();
const localeTag = useLocaleTag();

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
  const currencyFmt = new Intl.NumberFormat(localeTag.value, {style: 'currency', currency: props.currency});

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
          label: (context) => currencyFmt.format(context.parsed),
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
