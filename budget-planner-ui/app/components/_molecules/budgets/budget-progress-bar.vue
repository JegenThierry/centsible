<script lang="ts" setup>
import type {Budget} from "~/models/budget/budget";
import {Currency} from "~/models/budget-account/currency";
import BalanceNumberFormat from "~/components/_molecules/labels/balance-number-format.vue";

const props = defineProps<{
  budget: Budget;
  currency?: Currency;
}>();

const {t} = useI18n();

const ratio = computed(() => {
  if (props.budget.amountLimit <= 0) return 0;
  return props.budget.amountSpent / props.budget.amountLimit;
});

const percent = computed(() => Math.min(100, Math.round(ratio.value * 100)));
const overBudget = computed(() => ratio.value > 1);

const barColor = computed(() => {
  if (overBudget.value) return 'bg-error';
  if (ratio.value >= 0.85) return 'bg-warning';
  return 'bg-success';
});

const cur = computed(() => props.currency ?? Currency.EUR);
</script>

<template>
  <div class="space-y-2">
    <div class="flex items-center justify-between gap-2">
      <div class="flex items-center gap-2 min-w-0">
        <UIcon :name="budget.category.icon"
               :style="{color: budget.category.color}"
               class="w-4 h-4 shrink-0"/>
        <span class="font-medium truncate">{{ budget.category.name }}</span>
        <UBadge v-if="overBudget" color="error" size="sm" variant="subtle">{{ t('budgets.list.overBadge') }}</UBadge>
      </div>
      <div class="text-sm tabular-nums whitespace-nowrap">
        <BalanceNumberFormat :balance="budget.amountSpent" :currency="cur"/>
        <span class="text-neutral-400 mx-1">/</span>
        <BalanceNumberFormat :balance="budget.amountLimit" :currency="cur"/>
      </div>
    </div>
    <div class="h-2 w-full rounded-full bg-elevated overflow-hidden">
      <div :class="barColor"
           :style="{width: percent + '%'}"
           class="h-full transition-all"/>
    </div>
  </div>
</template>
