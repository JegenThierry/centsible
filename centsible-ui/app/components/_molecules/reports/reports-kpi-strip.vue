<script lang="ts" setup>
import type {CashFlowPoint} from "~/models/reports/cash-flow";
import type {Currency} from "~/models/budget-account/currency";
import BalanceNumberFormat from "~/components/_atoms/labels/balance-number-format.vue";

const props = defineProps<{
  current: CashFlowPoint[];
  previous: CashFlowPoint[];
  currency: Currency;
}>();

const {t, locale} = useI18n();

interface Totals {
  income: number;
  expense: number;
  net: number;
  /** net / income — null when there's no income to divide by. */
  savingsRate: number | null;
}

function totalsOf(points: CashFlowPoint[]): Totals {
  const income = points.reduce((s, p) => s + Number(p.income), 0);
  const expense = points.reduce((s, p) => s + Number(p.expense), 0);
  const net = income - expense;
  return {income, expense, net, savingsRate: income > 0 ? net / income : null};
}

const current = computed(() => totalsOf(props.current));
const previous = computed(() => totalsOf(props.previous));
const hasComparison = computed(() => props.previous.length > 0);

const percentFmt = computed(() =>
  new Intl.NumberFormat(locale.value, {style: 'percent', maximumFractionDigits: 0}),
);

interface Kpi {
  key: string;
  label: string;
  /** Currency KPIs carry an amount; the savings-rate KPI carries a ratio (or null). */
  value: number | null;
  isCurrency: boolean;
  /** Change vs previous period: a signed ratio for currency KPIs, percentage-points for the rate. */
  change: number | null;
  unit: 'percent' | 'points';
  positiveIsGood: boolean;
}

/** Relative change as a signed ratio (0.12 = +12%), or null when the baseline is zero. */
function ratioChange(cur: number, prev: number): number | null {
  return prev === 0 ? null : (cur - prev) / Math.abs(prev);
}

const kpis = computed<Kpi[]>(() => [
  {
    key: 'income',
    label: t('reports.kpi.income'),
    value: current.value.income,
    isCurrency: true,
    change: ratioChange(current.value.income, previous.value.income),
    unit: 'percent',
    positiveIsGood: true,
  },
  {
    key: 'expense',
    label: t('reports.kpi.expense'),
    value: current.value.expense,
    isCurrency: true,
    change: ratioChange(current.value.expense, previous.value.expense),
    unit: 'percent',
    positiveIsGood: false,
  },
  {
    key: 'net',
    label: t('reports.kpi.net'),
    value: current.value.net,
    isCurrency: true,
    change: ratioChange(current.value.net, previous.value.net),
    unit: 'percent',
    positiveIsGood: true,
  },
  {
    key: 'savings',
    label: t('reports.kpi.savingsRate'),
    value: current.value.savingsRate,
    isCurrency: false,
    change: current.value.savingsRate != null && previous.value.savingsRate != null
      ? current.value.savingsRate - previous.value.savingsRate
      : null,
    unit: 'points',
    positiveIsGood: true,
  },
]);

function changeTone(kpi: Kpi): 'good' | 'bad' | 'neutral' {
  if (kpi.change == null || kpi.change === 0) return 'neutral';
  return (kpi.change > 0) === kpi.positiveIsGood ? 'good' : 'bad';
}

const toneClass: Record<'good' | 'bad' | 'neutral', string> = {
  good: 'text-success',
  bad: 'text-error',
  neutral: 'text-muted',
};

function changeIcon(kpi: Kpi): string {
  if (kpi.change == null || kpi.change === 0) return 'i-lucide-minus';
  return kpi.change > 0 ? 'i-lucide-arrow-up-right' : 'i-lucide-arrow-down-right';
}

function changeText(kpi: Kpi): string {
  if (kpi.change == null) return t('reports.kpi.noComparison');
  const sign = kpi.change > 0 ? '+' : '';
  if (kpi.unit === 'points') {
    return `${sign}${(kpi.change * 100).toFixed(1)} ${t('reports.kpi.points')}`;
  }
  return `${sign}${percentFmt.value.format(kpi.change)}`;
}

function formatRate(ratio: number | null): string {
  return ratio == null ? '—' : percentFmt.value.format(ratio);
}
</script>

<template>
  <div class="grid grid-cols-2 lg:grid-cols-4 gap-4 sm:gap-6">
    <UCard v-for="kpi in kpis" :key="kpi.key">
      <p class="text-xs uppercase tracking-wide text-muted">{{ kpi.label }}</p>
      <p class="mt-1 text-xl font-semibold tabular-nums text-highlighted">
        <BalanceNumberFormat v-if="kpi.isCurrency" :balance="kpi.value ?? 0" :currency="currency"/>
        <span v-else>{{ formatRate(kpi.value) }}</span>
      </p>
      <p v-if="hasComparison"
         class="mt-1 flex items-center gap-1 text-xs"
         :class="toneClass[changeTone(kpi)]"
         :title="t('reports.kpi.vsPrevious')">
        <UIcon :name="changeIcon(kpi)" class="w-3.5 h-3.5 shrink-0"/>
        <span class="tabular-nums">{{ changeText(kpi) }}</span>
      </p>
    </UCard>
  </div>
</template>
