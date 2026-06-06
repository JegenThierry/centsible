<script lang="ts" setup>
import type {Budget} from "~/models/budget/budget";
import {Currency} from "~/models/budget-account/currency";
import BalanceNumberFormat from "~/components/_atoms/labels/balance-number-format.vue";

const props = defineProps<{
  budget: Budget;
  currency?: Currency;
}>();

const {t} = useI18n();

const effectiveLimit = computed(() =>
  props.budget.amountLimit + (props.budget.rolloverAmount ?? 0)
);

const ratio = computed(() => effectiveLimit.value > 0
  ? props.budget.amountSpent / effectiveLimit.value
  : 0);

const percent = computed(() => Math.min(100, Math.round(ratio.value * 100)));
const overBudget = computed(() => ratio.value > 1);

const barColor = computed(() => {
  if (overBudget.value) return 'bg-error';
  if (ratio.value >= 0.85) return 'bg-warning';
  return 'bg-success';
});

const resolvedCurrency = computed(() => props.currency ?? Currency.EUR);
const periodLabel = computed(() => t(`budgets.periods.${props.budget.periodType ?? 'MONTHLY'}`));

const localeTag = useLocaleTag();
const rolloverFormatted = computed(() => new Intl.NumberFormat(localeTag.value, {
  style: 'currency',
  currency: resolvedCurrency.value,
}).format(props.budget.rolloverAmount ?? 0));
</script>

<template>
  <div class="space-y-2">
    <div class="flex items-center justify-between gap-2">
      <div class="flex items-center gap-2 min-w-0 flex-wrap">
        <UIcon :name="budget.category.icon"
               :style="{color: budget.category.color}"
               class="w-4 h-4 shrink-0"/>
        <span class="font-medium truncate">{{ budget.category.name }}</span>
        <UBadge color="neutral" size="sm" variant="subtle">{{ periodLabel }}</UBadge>
        <UBadge v-if="budget.rolloverEnabled && budget.rolloverAmount > 0"
                color="info"
                size="sm"
                variant="subtle">
          +<BalanceNumberFormat :balance="budget.rolloverAmount" :currency="resolvedCurrency"/>
        </UBadge>
        <UBadge v-if="overBudget" color="error" size="sm" variant="subtle">{{ t('budgets.list.overBadge') }}</UBadge>
      </div>
      <div class="text-sm tabular-nums whitespace-nowrap">
        <BalanceNumberFormat :balance="budget.amountSpent" :currency="resolvedCurrency"/>
        <span class="text-neutral-400 mx-1">/</span>
        <BalanceNumberFormat :balance="effectiveLimit" :currency="resolvedCurrency"/>
      </div>
    </div>
    <div class="h-2 w-full rounded-full bg-elevated overflow-hidden">
      <div :class="barColor"
           :style="{width: percent + '%'}"
           class="h-full transition-all"/>
    </div>
    <p v-if="budget.rolloverEnabled && budget.rolloverAmount > 0"
       class="text-xs text-muted">
      {{ t('budgets.list.rolloverNote', {amount: rolloverFormatted}) }}
    </p>
  </div>
</template>
