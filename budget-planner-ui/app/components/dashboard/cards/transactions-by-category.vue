<script setup lang="ts">
import {
  Chart as ChartJS,
  ArcElement,
  Tooltip,
  Legend,
  type ChartOptions,
  type ChartData
} from 'chart.js';
import {Doughnut} from 'vue-chartjs';
import {type Transaction, TransactionType} from "~/models/transactions/transaction";
import type {Currency} from "~/models/budget-account/currency";
import BalanceNumberFormat from "~/components/_molecules/labels/balance-number-format.vue";

ChartJS.register(ArcElement, Tooltip, Legend);

const props = defineProps<{
  transactions: Transaction[],
  currency: Currency
}>();

const {getCategoryColor} = useColorCalculator();

const expensesByCategory = computed(() => {
  const categories: Record<string, { total: number, color: string }> = {};

  if (!props.transactions) return categories;

  const expenseTransactions = props.transactions
    .filter(transaction => transaction && transaction.type === TransactionType.EXPENSE && transaction.category);

  const distinctCategories = Array.from(new Set(expenseTransactions.map(t => t.category.id)));

  expenseTransactions.forEach(transaction => {
    const categoryName = transaction.category.name || 'Unknown';
    const categoryId = transaction.category.id;

    if (!categories[categoryName]) {
      const categoryIndex = distinctCategories.indexOf(categoryId);
      const color = getCategoryColor(categoryIndex, distinctCategories.length);
      categories[categoryName] = {total: 0, color};
    }
    categories[categoryName].total += transaction.amount || 0;
  });

  return categories;
});

const chartData = computed<ChartData<'doughnut'>>(() => {
  const labels = Object.keys(expensesByCategory.value);
  const data = Object.values(expensesByCategory.value).map(v => Math.abs(v.total));
  const backgroundColors = Object.values(expensesByCategory.value).map(v => v.color || '#a3a3a3');

  return {
    labels,
    datasets: [
      {
        data,
        backgroundColor: backgroundColors,
        borderWidth: 0,
        hoverOffset: 4
      }
    ]
  };
});

const chartOptions = computed<ChartOptions<'doughnut'>>(() => ({
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: {
      position: 'bottom',
      labels: {
        color: '#a3a3a3',
        padding: 20,
        usePointStyle: true,
        font: {
          size: 11
        }
      }
    },
    tooltip: {
      callbacks: {
        label: (context) => {
          const value = context.parsed;
          return new Intl.NumberFormat('de-DE', {
            style: 'currency',
            currency: props.currency
          }).format(value);
        }
      }
    }
  },
  cutout: '70%'
}));

const totalExpenses = computed(() => {
  return Math.abs(Object.values(expensesByCategory.value).reduce((sum, current) => sum + current.total, 0));
});
</script>

<template>
  <UCard>
    <template #header>
      <h3 class="text-base font-semibold text-white">
        Expenses by Category
      </h3>
    </template>

    <div class="relative h-64">
      <Doughnut :data="chartData" :options="chartOptions"/>

      <div class="absolute inset-0 flex flex-col items-center justify-center pointer-events-none mb-10">
        <span class="text-xs text-neutral-500 uppercase tracking-widest font-medium">Total</span>
        <span class="text-lg font-bold text-white">
          <BalanceNumberFormat :balance="totalExpenses"
                               :currency="currency"
                               format="de-De"/>
        </span>
      </div>
    </div>
  </UCard>
</template>
