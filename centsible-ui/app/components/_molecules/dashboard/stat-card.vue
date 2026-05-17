<script lang="ts" setup>
import type {Currency} from "~/models/budget-account/currency";
import BalanceNumberFormat from "~/components/_atoms/labels/balance-number-format.vue";

type StatColor = 'primary' | 'success' | 'error' | 'warning' | 'info';
type StatTone = 'positive' | 'negative' | 'neutral' | 'signed';

const props = withDefaults(defineProps<{
  label: string;
  amount: number;
  currency: Currency;
  icon: string;
  color: StatColor;
  tone?: StatTone;
}>(), {
  tone: 'neutral',
});

const iconClass: Record<StatColor, string> = {
  primary: 'bg-primary-100 dark:bg-primary-900/30 text-primary',
  success: 'bg-success-100 dark:bg-success-900/30 text-success',
  error: 'bg-error-100 dark:bg-error-900/30 text-error',
  warning: 'bg-warning-100 dark:bg-warning-900/30 text-warning',
  info: 'bg-info-100 dark:bg-info-900/30 text-info',
};

const amountClass = computed(() => {
  switch (props.tone) {
    case 'positive': return 'text-success';
    case 'negative': return 'text-error';
    case 'signed': return props.amount >= 0 ? 'text-success' : 'text-error';
    default: return 'text-highlighted';
  }
});
</script>

<template>
  <UCard>
    <div class="flex items-center gap-4">
      <div :class="['p-3 rounded-full shrink-0', iconClass[color]]">
        <UIcon :name="icon" class="w-6 h-6 flex my-auto"/>
      </div>
      <div class="flex flex-col overflow-hidden">
        <span class="text-xs text-muted uppercase tracking-widest font-medium truncate">
          {{ label }}
        </span>
        <span :class="['text-xl font-bold mt-1 truncate', amountClass]">
          <BalanceNumberFormat :balance="amount" :currency="currency"/>
        </span>
      </div>
    </div>
  </UCard>
</template>
