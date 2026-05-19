<script lang="ts" setup>
import BalanceNumberFormat from "~/components/_atoms/labels/balance-number-format.vue";
import type {YearOverYear} from "~/models/reports/year-over-year";
import type {Currency} from "~/models/budget-account/currency";

const props = defineProps<{
  data: YearOverYear | null;
  currency: Currency;
}>();

const {t} = useI18n();

function pct(current: number, previous: number): number | null {
  if (!previous) return null;
  return ((current - previous) / Math.abs(previous)) * 100;
}

function changeClass(value: number, lessIsBetter: boolean): string {
  if (value === 0) return 'text-muted';
  const positive = value > 0;
  if (lessIsBetter) return positive ? 'text-error' : 'text-success';
  return positive ? 'text-success' : 'text-error';
}

const topCategories = computed(() => (props.data?.perCategory ?? []).slice(0, 6));
</script>

<template>
  <UCard>
    <template #header>
      <h3 class="text-base font-semibold text-highlighted">{{ t('reports.yearOverYear.title') }}</h3>
    </template>

    <template v-if="data">
      <div class="grid grid-cols-2 gap-3">
        <div class="rounded-md ring-1 ring-default p-3">
          <p class="text-xs text-muted">{{ t('reports.yearOverYear.income') }}</p>
          <p class="text-lg font-bold">
            <BalanceNumberFormat :balance="Number(data.totals.thisYearIncome)" :currency="currency"/>
          </p>
          <p class="text-xs text-muted">
            {{ t('reports.yearOverYear.lastYear', {year: data.lastYear}) }}:
            <BalanceNumberFormat :balance="Number(data.totals.lastYearIncome)" :currency="currency"/>
          </p>
          <p v-if="pct(Number(data.totals.thisYearIncome), Number(data.totals.lastYearIncome)) !== null"
             :class="['text-xs font-medium mt-1', changeClass(pct(Number(data.totals.thisYearIncome), Number(data.totals.lastYearIncome))!, false)]">
            {{ pct(Number(data.totals.thisYearIncome), Number(data.totals.lastYearIncome))!.toFixed(1) }}%
          </p>
        </div>

        <div class="rounded-md ring-1 ring-default p-3">
          <p class="text-xs text-muted">{{ t('reports.yearOverYear.expense') }}</p>
          <p class="text-lg font-bold">
            <BalanceNumberFormat :balance="Number(data.totals.thisYearExpense)" :currency="currency"/>
          </p>
          <p class="text-xs text-muted">
            {{ t('reports.yearOverYear.lastYear', {year: data.lastYear}) }}:
            <BalanceNumberFormat :balance="Number(data.totals.lastYearExpense)" :currency="currency"/>
          </p>
          <p v-if="pct(Number(data.totals.thisYearExpense), Number(data.totals.lastYearExpense)) !== null"
             :class="['text-xs font-medium mt-1', changeClass(pct(Number(data.totals.thisYearExpense), Number(data.totals.lastYearExpense))!, true)]">
            {{ pct(Number(data.totals.thisYearExpense), Number(data.totals.lastYearExpense))!.toFixed(1) }}%
          </p>
        </div>
      </div>

      <div v-if="topCategories.length > 0" class="mt-4">
        <p class="text-xs font-semibold text-muted uppercase tracking-wide mb-2">
          {{ t('reports.yearOverYear.perCategory') }}
        </p>
        <ul class="space-y-1.5">
          <li v-for="cat in topCategories" :key="cat.categoryId" class="flex items-center gap-3 text-sm">
            <span class="w-2.5 h-2.5 rounded-full shrink-0"
                  :style="{backgroundColor: cat.categoryColor ?? '#a3a3a3'}"/>
            <span class="flex-1 truncate">{{ cat.categoryName }}</span>
            <span class="tabular-nums text-muted text-xs">
              <BalanceNumberFormat :balance="Number(cat.lastYearAmount)" :currency="currency"/>
            </span>
            <UIcon name="i-lucide-arrow-right" class="text-muted shrink-0"/>
            <span class="tabular-nums font-medium">
              <BalanceNumberFormat :balance="Number(cat.thisYearAmount)" :currency="currency"/>
            </span>
            <span v-if="pct(Number(cat.thisYearAmount), Number(cat.lastYearAmount)) !== null"
                  :class="['text-xs w-14 text-right', changeClass(pct(Number(cat.thisYearAmount), Number(cat.lastYearAmount))!, true)]">
              {{ pct(Number(cat.thisYearAmount), Number(cat.lastYearAmount))!.toFixed(0) }}%
            </span>
          </li>
        </ul>
      </div>
    </template>

    <div v-else class="text-sm text-muted py-4 text-center">
      {{ t('reports.yearOverYear.empty') }}
    </div>
  </UCard>
</template>
