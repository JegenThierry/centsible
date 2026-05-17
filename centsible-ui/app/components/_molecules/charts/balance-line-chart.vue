<script lang="ts" setup>
import {
  CategoryScale,
  Chart as ChartJS,
  type ChartData,
  type ChartOptions,
  Filler,
  Legend,
  LinearScale,
  LineElement,
  PointElement,
  Title,
  Tooltip
} from 'chart.js';
import {Line} from 'vue-chartjs';
import type {Currency} from "~/models/budget-account/currency";
import {parseISO} from 'date-fns';

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

const props = withDefaults(defineProps<{
  points: { date: string; balance: number }[];
  currency: Currency;
  color: string;
  legendLabel: string;
  fillColor?: string;
  tension?: number;
  heightClass?: string;
}>(), {
  tension: 0.4,
  heightClass: 'h-64',
});

const colorMode = useColorMode();
const localeTag = useLocaleTag();

const chartData = computed<ChartData<'line'>>(() => {
  const sorted = [...(props.points ?? [])]
    .filter(p => p && p.date)
    .sort((a, b) => new Date(a.date).getTime() - new Date(b.date).getTime());

  if (sorted.length === 0) return {labels: [], datasets: []};

  const isDark = colorMode.value === 'dark';
  const dateFmt = new Intl.DateTimeFormat(localeTag.value, {day: '2-digit', month: '2-digit', year: 'numeric'});
  return {
    labels: sorted.map(p => dateFmt.format(parseISO(p.date))),
    datasets: [
      {
        label: props.legendLabel,
        backgroundColor: props.fillColor ?? 'rgba(16, 185, 129, 0.1)',
        borderColor: props.color,
        borderWidth: 2,
        pointBackgroundColor: props.color,
        pointBorderColor: isDark ? '#171717' : '#fff',
        pointBorderWidth: 1,
        pointRadius: 3,
        pointHoverRadius: 6,
        pointHoverBackgroundColor: props.color,
        pointHoverBorderColor: isDark ? '#171717' : '#fff',
        pointHoverBorderWidth: 2,
        data: sorted.map(p => p.balance),
        tension: props.tension,
        fill: true,
      }
    ]
  };
});

const chartOptions = computed<ChartOptions<'line'>>(() => {
  const isDark = colorMode.value === 'dark';
  const gridColor = isDark ? '#262626' : '#e5e5e5';
  const tickColor = isDark ? '#a3a3a3' : '#737373';
  const currencyFmt = new Intl.NumberFormat(localeTag.value, {
    style: 'currency',
    currency: props.currency,
    maximumFractionDigits: 0,
  });

  return {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {display: false},
      tooltip: {
        callbacks: {
          label: (context) => currencyFmt.format(context.parsed.y as number)
        }
      }
    },
    scales: {
      y: {
        grid: {color: gridColor},
        ticks: {
          color: tickColor,
          callback: (value) => currencyFmt.format(value as number)
        }
      },
      x: {
        grid: {display: false},
        ticks: {color: tickColor}
      }
    }
  };
});
</script>

<template>
  <div :class="heightClass">
    <Line :data="chartData" :options="chartOptions"/>
  </div>
</template>
