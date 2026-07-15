<script lang="ts" setup>
import type {ChartData, ChartOptions} from 'chart.js';
import {Line} from 'vue-chartjs';
import '~/utils/chart-registry';
import type {Currency} from "~/models/budget-account/currency";
import {parseISO} from 'date-fns';
import {useChartTheme} from "~/composables/use-chart-theme";

const props = withDefaults(defineProps<{
  points: { date: string; balance: number }[];
  currency: Currency;
  color: string;
  legendLabel: string;
  fillColor?: string;
  tension?: number;
  heightClass?: string;
  /** Render the line dashed — used to signal a projected/forecast series. */
  dashed?: boolean;
}>(), {
  tension: 0,
  heightClass: 'h-64',
  dashed: false,
});

const emit = defineEmits<{
  'point-click': [date: string];
}>();

const localeTag = useLocaleTag();
const {isDark, tickColor, gridColor, currencyFmt, pointerCursorOnHover} = useChartTheme(() => props.currency);

const sortedPoints = computed(() =>
  [...(props.points ?? [])]
    .filter(p => p && p.date)
    .sort((a, b) => new Date(a.date).getTime() - new Date(b.date).getTime()),
);

const chartData = computed<ChartData<'line'>>(() => {
  const sorted = sortedPoints.value;
  if (sorted.length === 0) return {labels: [], datasets: []};

  const dateFmt = new Intl.DateTimeFormat(localeTag.value, {day: '2-digit', month: '2-digit', year: 'numeric'});
  const pointBorder = isDark.value ? '#171717' : '#fff';
  const showDots = sorted.length <= 40;
  return {
    labels: sorted.map(p => dateFmt.format(parseISO(p.date))),
    datasets: [
      {
        label: props.legendLabel,
        backgroundColor: props.fillColor ?? 'rgba(16, 185, 129, 0.1)',
        borderColor: props.color,
        borderWidth: 2,
        borderDash: props.dashed ? [6, 5] : [],
        pointBackgroundColor: props.color,
        pointBorderColor: pointBorder,
        pointBorderWidth: 1,
        pointRadius: showDots ? 3 : 0,
        pointHoverRadius: 6,
        pointHoverBackgroundColor: props.color,
        pointHoverBorderColor: pointBorder,
        pointHoverBorderWidth: 2,
        data: sorted.map(p => p.balance),
        tension: props.tension,
        fill: true,
      }
    ]
  };
});

const chartOptions = computed<ChartOptions<'line'>>(() => {
  const fmt = currencyFmt();
  const pts = sortedPoints.value;
  const spanDays = pts.length > 1
    ? (parseISO(pts[pts.length - 1]!.date).getTime() - parseISO(pts[0]!.date).getTime()) / 86_400_000
    : 0;
  const tickFmt = new Intl.DateTimeFormat(
    localeTag.value,
    spanDays > 100 ? {month: 'short', year: '2-digit'} : {day: '2-digit', month: 'short'},
  );
  return {
    responsive: true,
    maintainAspectRatio: false,
    interaction: {mode: 'index', intersect: false},
    plugins: {
      legend: {display: false},
      tooltip: {
        callbacks: {
          label: (context) => fmt.format(context.parsed.y as number)
        }
      }
    },
    scales: {
      y: {
        grid: {color: gridColor.value},
        ticks: {
          color: tickColor.value,
          callback: (value) => fmt.format(value as number)
        }
      },
      x: {
        grid: {display: false},
        ticks: {
          color: tickColor.value,
          maxRotation: 0,
          autoSkip: true,
          maxTicksLimit: 12,
          callback: (value) => {
            const p = sortedPoints.value[Number(value)];
            return p ? tickFmt.format(parseISO(p.date)) : '';
          },
        }
      }
    },
    onClick: (_evt, elements) => {
      const idx = elements?.[0]?.index;
      if (idx == null) return;
      const date = sortedPoints.value[idx]?.date;
      if (date) emit('point-click', date);
    },
    onHover: pointerCursorOnHover,
  };
});
</script>

<template>
  <div :class="heightClass">
    <Line :data="chartData" :options="chartOptions"/>
  </div>
</template>
