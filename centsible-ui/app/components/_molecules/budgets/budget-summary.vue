<script lang="ts" setup>
import {type Budget, budgetBarColor, effectiveLimit} from "~/models/budget/budget";
import {Currency} from "~/models/budget-account/currency";
import BalanceNumberFormat from "~/components/_atoms/labels/balance-number-format.vue";
import {formatMonthYearLabel} from "~/utils/date";

const props = defineProps<{
  budgets: Budget[];
  currency?: Currency;
  month: string;
}>();

const {t} = useI18n();
const localeTag = useLocaleTag();

const resolvedCurrency = computed(() => props.currency ?? Currency.EUR);

const totalBudgeted = computed(() => props.budgets.reduce((sum, b) => sum + effectiveLimit(b), 0));
const totalSpent = computed(() => props.budgets.reduce((sum, b) => sum + b.amountSpent, 0));
const remaining = computed(() => totalBudgeted.value - totalSpent.value);
const absRemaining = computed(() => Math.abs(remaining.value));

const ratio = computed(() => totalBudgeted.value > 0 ? totalSpent.value / totalBudgeted.value : 0);
const rawPercent = computed(() => Math.round(ratio.value * 100));
const barPercent = computed(() => Math.min(100, rawPercent.value));

const overCount = computed(() => props.budgets.filter(b => b.amountSpent > effectiveLimit(b)).length);

const barColor = computed(() => budgetBarColor(ratio.value));

const monthLabel = computed(() => formatMonthYearLabel(props.month, localeTag.value));
</script>

<template>
  <UCard :ui="{body: 'p-4 sm:p-5'}" variant="outline">
    <div class="space-y-3">
      <div class="flex items-center justify-between gap-2">
        <h2 class="text-sm font-semibold text-highlighted capitalize">{{ monthLabel }}</h2>
        <span class="text-xs text-muted tabular-nums">{{ t('budgets.summary.used', {percent: rawPercent}) }}</span>
      </div>

      <div class="flex items-baseline justify-between gap-2 text-sm tabular-nums">
        <span>
          <BalanceNumberFormat :balance="totalSpent" :currency="resolvedCurrency" class="font-semibold text-highlighted"/>
          <span class="text-muted ml-1">{{ t('budgets.summary.spent') }}</span>
        </span>
        <span class="text-muted">
          <BalanceNumberFormat :balance="totalBudgeted" :currency="resolvedCurrency"/>
          <span class="ml-1">{{ t('budgets.summary.budgeted') }}</span>
        </span>
      </div>

      <UProgress :color="barColor" :model-value="barPercent" :ui="{base: 'h-2.5 bg-elevated'}" size="md"/>

      <div class="flex items-center justify-between gap-2 text-sm">
        <span :class="remaining >= 0 ? 'text-success' : 'text-error'" class="tabular-nums font-medium">
          <BalanceNumberFormat :balance="absRemaining" :currency="resolvedCurrency"/>
          <span class="ml-1">{{ remaining >= 0 ? t('budgets.summary.left') : t('budgets.summary.overBy') }}</span>
        </span>
        <span v-if="overCount > 0" class="flex items-center gap-1 text-error whitespace-nowrap">
          <UIcon name="i-lucide-triangle-alert" class="w-4 h-4 shrink-0"/>
          {{ t('budgets.summary.overCount', {over: overCount, total: budgets.length}) }}
        </span>
        <span v-else class="flex items-center gap-1 text-success whitespace-nowrap">
          <UIcon name="i-lucide-check" class="w-4 h-4 shrink-0"/>
          {{ t('budgets.summary.allWithin', {total: budgets.length}) }}
        </span>
      </div>
    </div>
  </UCard>
</template>
