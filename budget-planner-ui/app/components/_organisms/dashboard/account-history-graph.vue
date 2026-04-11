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
  Filler,
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
  CategoryScale,
  Filler
);

const props = defineProps<{
  snapshots: BudgetAccountSnapshot[],
  currency: Currency
}>();

const colorMode = useColorMode();

const chartData = computed<ChartData<'line'>>(() => {
  const isDark = colorMode.value === 'dark';

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
        backgroundColor: 'rgba(16, 185, 129, 0.1)',
        borderColor: '#10b981',
        borderWidth: 2,
        pointBackgroundColor: '#10b981',
        pointBorderColor: isDark ? '#171717' : '#fff',
        pointBorderWidth: 1,
        pointRadius: 3,
        pointHoverRadius: 6,
        pointHoverBackgroundColor: '#10b981',
        pointHoverBorderColor: isDark ? '#171717' : '#fff',
        pointHoverBorderWidth: 2,
        data: sorted.map(s => s.balance),
        tension: 0.4,
        fill: true,
      }
    ]
  };
});

const chartOptions = computed<ChartOptions<'line'>>(() => {
  const isDark = colorMode.value === 'dark';
  const gridColor = isDark ? '#262626' : '#e5e5e5';
  const tickColor = isDark ? '#a3a3a3' : '#737373';

  return {
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
          color: gridColor
        },
        ticks: {
          color: tickColor,
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
          color: tickColor
        }
      }
    }
  };
});
</script>

<template>
  <UCard>
    <template #header>
      <div class="flex items-center justify-between">
        <h3 class="text-base font-semibold text-gray-900 dark:text-white">
          Balance Over Time
        </h3>
      </div>
    </template>

    <div class="h-64">
      <Line :data="chartData" :options="chartOptions" />
    </div>
  </UCard>
</template>
