<script setup lang="ts">
import {
  Chart as ChartJS,
  Title,
  Tooltip,
  Legend,
  LineElement,
  LinearScale,
  PointElement,
  CategoryScale,
  type ChartOptions,
  type ChartData
} from 'chart.js';
import {Line} from 'vue-chartjs';
import type {BudgetAccountSnapshot} from "~/models/budget-account/budget-account";
import type {Currency} from "~/models/budget-account/currency";
import {format, parseISO} from 'date-fns';

ChartJS.register(
  Title,
  Tooltip,
  Legend,
  LineElement,
  LinearScale,
  PointElement,
  CategoryScale
);

const props = defineProps<{
  snapshots: BudgetAccountSnapshot[],
  currency: Currency
}>();

const chartData = computed<ChartData<'line'>>(() => {
  if (!props.snapshots || props.snapshots.length === 0) {
    return {
      labels: [],
      datasets: []
    };
  }

  const sorted = [...props.snapshots]
    .filter(s => s && s.createdAt)
    .sort((a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime());

  if (sorted.length === 0) {
    return {
      labels: [],
      datasets: []
    };
  }

  return {
    labels: sorted.map(s => format(parseISO(s.createdAt), 'dd.MM.yyyy')),
    datasets: [
      {
        label: 'Balance',
        backgroundColor: '#10b981',
        borderColor: '#10b981',
        data: sorted.map(s => s.balance),
        tension: 0.4,
        pointRadius: 2,
      }
    ]
  };
});

const chartOptions = computed<ChartOptions<'line'>>(() => ({
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: {
      display: false
    },
    tooltip: {
      callbacks: {
        label: (context) => {
          return new Intl.NumberFormat('de-DE', {
            style: 'currency',
            currency: props.currency,
            maximumFractionDigits: 0
          }).format(context.parsed.y as number);
        }
      }
    }
  },
  scales: {
    y: {
      grid: {
        color: '#262626'
      },
      ticks: {
        color: '#a3a3a3',
        callback: (value) => {
           return new Intl.NumberFormat('de-DE', {
            style: 'currency',
            currency: props.currency,
            maximumFractionDigits: 0
          }).format(value as number);
        }
      }
    },
    x: {
      grid: {
        display: false
      },
      ticks: {
        color: '#a3a3a3'
      }
    }
  }
}));
</script>

<template>
  <UCard>
    <template #header>
      <div class="flex items-center justify-between">
        <h3 class="text-base font-semibold text-white">
          Balance Over Time
        </h3>
      </div>
    </template>

    <div class="h-64">
      <Line :data="chartData" :options="chartOptions" />
    </div>
  </UCard>
</template>
