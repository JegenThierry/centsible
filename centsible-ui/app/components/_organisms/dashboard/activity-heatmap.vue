<script lang="ts" setup>
import {addDays, addWeeks, format, isAfter, startOfWeek, subWeeks} from 'date-fns';
import type {Currency} from "~/models/budget-account/currency";
import {useChartTheme} from "~/composables/use-chart-theme";
import {useDailyAggregates} from "~/composables/use-daily-aggregates";

const props = defineProps<{
  accountId: string;
  currency: Currency;
}>();

const WEEKS = 53;
const DAYS = 371;

const SHADES_DARK = [800, 600, 500, 400];
const SHADES_LIGHT = [200, 400, 600, 700];

const COLS_STYLE = 'grid-template-columns: repeat(53, minmax(0, 1fr))';
const CELLS_STYLE = `${COLS_STYLE}; grid-template-rows: repeat(7, auto); grid-auto-flow: column`;

interface Cell {
  key: string;
  date: Date | null;
  future: boolean;
  income: number;
  expense: number;
  net: number;
}

const {t} = useI18n();
const localeTag = useLocaleTag();
const {isDark, currencyFmt} = useChartTheme(() => props.currency);

const {data, loading} = useDailyAggregates(() => props.accountId, () => DAYS);

const mounted = useMounted();

const byDate = computed(() => {
  const map = new Map<string, {income: number; expense: number; net: number}>();
  for (const a of data.value) {
    const income = Number(a.income) || 0;
    const expense = Number(a.expense) || 0;
    map.set(a.date, {income, expense, net: income - expense});
  }
  return map;
});

const maxAbs = computed(() => {
  let max = 0;
  for (const v of byDate.value.values()) {
    const abs = Math.abs(v.net);
    if (abs > max) max = abs;
  }
  return max;
});

const weeks = computed<Cell[][]>(() => {
  if (!mounted.value) return [];
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const firstWeekStart = subWeeks(startOfWeek(today, {weekStartsOn: 1}), WEEKS - 1);

  const grid: Cell[][] = [];
  for (let w = 0; w < WEEKS; w++) {
    const weekStart = addWeeks(firstWeekStart, w);
    const column: Cell[] = [];
    for (let d = 0; d < 7; d++) {
      const day = addDays(weekStart, d);
      const future = isAfter(day, today);
      const agg = future ? undefined : byDate.value.get(format(day, 'yyyy-MM-dd'));
      column.push({
        key: `${w}-${d}`,
        date: future ? null : day,
        future,
        income: agg?.income ?? 0,
        expense: agg?.expense ?? 0,
        net: agg?.net ?? 0,
      });
    }
    grid.push(column);
  }
  return grid;
});

const flatCells = computed(() => weeks.value.flat());

const weekdayLabels = computed<string[]>(() => {
  const column = weeks.value[0];
  if (!column) return [];
  return column.map((c, i) =>
    i % 2 === 0 && i < 6 && c.date ? c.date.toLocaleDateString(localeTag.value, {weekday: 'short'}) : '',
  );
});

const columnMonthLabels = computed<string[]>(() => {
  const labels: string[] = [];
  let lastMonth = -1;
  for (const column of weeks.value) {
    const day = (column.find(c => c.date) ?? column[0])?.date ?? null;
    if (day && day.getMonth() !== lastMonth) {
      labels.push(day.toLocaleDateString(localeTag.value, {month: 'short'}));
      lastMonth = day.getMonth();
    } else {
      labels.push('');
    }
  }
  return labels;
});

function shadeFor(level: number): number {
  const ramp = isDark.value ? SHADES_DARK : SHADES_LIGHT;
  return ramp[level] ?? 500;
}

function levelIndex(absNet: number): number {
  const m = maxAbs.value;
  if (m <= 0) return 0;
  const r = absNet / m;
  if (r > 0.75) return 3;
  if (r > 0.5) return 2;
  if (r > 0.25) return 1;
  return 0;
}

const legendSpent = computed(() =>
  [3, 2, 1, 0].map(l => `var(--ui-color-error-${shadeFor(l)})`),
);
const legendEarned = computed(() =>
  [0, 1, 2, 3].map(l => `var(--ui-color-success-${shadeFor(l)})`),
);

function cellClass(cell: Cell): string {
  if (cell.future) return 'invisible';
  if (cell.net === 0) return 'bg-neutral-200 dark:bg-neutral-800';
  return '';
}

function cellStyle(cell: Cell): Record<string, string> {
  if (cell.future || cell.net === 0) return {};
  const name = cell.net > 0 ? 'success' : 'error';
  return {backgroundColor: `var(--ui-color-${name}-${shadeFor(levelIndex(Math.abs(cell.net)))})`};
}

function cellTitle(cell: Cell, dfmt: Intl.DateTimeFormat, cfmt: Intl.NumberFormat): string {
  if (cell.future || !cell.date) return '';
  const dateLabel = dfmt.format(cell.date);
  if (cell.income === 0 && cell.expense === 0) {
    return `${dateLabel} — ${t('accounts.dashboard.activityNoData')}`;
  }
  const sign = cell.net > 0 ? '+' : '';
  return `${dateLabel}: ${sign}${cfmt.format(cell.net)} `
    + `(${t('accounts.dashboard.income')} ${cfmt.format(cell.income)}, ${t('accounts.dashboard.expense')} ${cfmt.format(cell.expense)})`;
}

const renderCells = computed(() => {
  const dfmt = new Intl.DateTimeFormat(localeTag.value, {year: 'numeric', month: 'short', day: 'numeric'});
  const cfmt = currencyFmt(2);
  return flatCells.value.map(cell => ({
    key: cell.key,
    class: cellClass(cell),
    style: cellStyle(cell),
    title: cellTitle(cell, dfmt, cfmt),
  }));
});
</script>

<template>
  <UCard>
    <template #header>
      <div class="flex items-start justify-between gap-4">
        <div>
          <h3 class="text-base font-semibold text-highlighted">{{ t('accounts.dashboard.activityTitle') }}</h3>
          <p class="text-xs text-muted mt-0.5">{{ t('accounts.dashboard.activitySubtitle') }}</p>
        </div>
        <div v-if="mounted" class="hidden sm:flex items-center gap-1 text-xs text-muted shrink-0">
          <span class="mr-1">{{ t('accounts.dashboard.activitySpent') }}</span>
          <span v-for="(c, i) in legendSpent" :key="`s${i}`" class="h-3 w-3 rounded-sm" :style="{backgroundColor: c}"/>
          <span class="h-3 w-3 rounded-sm bg-neutral-200 dark:bg-neutral-800"/>
          <span v-for="(c, i) in legendEarned" :key="`e${i}`" class="h-3 w-3 rounded-sm" :style="{backgroundColor: c}"/>
          <span class="ml-1">{{ t('accounts.dashboard.activityEarned') }}</span>
        </div>
      </div>
    </template>

    <div
      v-if="!mounted || (loading && data.length === 0)"
      class="h-28 animate-pulse rounded-md bg-neutral-100 dark:bg-neutral-800/50"
    />
    <div v-else class="overflow-x-auto">
      <div class="min-w-[680px]">
        <div class="grid gap-[3px] mb-1 pl-8" :style="COLS_STYLE">
          <span
            v-for="(m, i) in columnMonthLabels"
            :key="`m${i}`"
            class="min-w-0 text-[10px] leading-none text-muted whitespace-nowrap"
          >{{ m }}</span>
        </div>

        <div class="flex gap-1 items-stretch">
          <div class="flex flex-col gap-[3px] w-7 shrink-0">
            <span
              v-for="(wd, i) in weekdayLabels"
              :key="`wd${i}`"
              class="flex-1 flex items-center justify-end text-[10px] leading-none text-muted"
            >{{ wd }}</span>
          </div>

          <div class="grid gap-[3px] flex-1 min-w-0" :style="CELLS_STYLE">
            <div
              v-for="cell in renderCells"
              :key="cell.key"
              class="aspect-square w-full rounded-sm"
              :class="cell.class"
              :style="cell.style"
              :title="cell.title"
            />
          </div>
        </div>
      </div>
    </div>
  </UCard>
</template>
