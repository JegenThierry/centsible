<script setup lang="ts">
import type {Currency} from "~/models/budget-account/currency";
import BalanceNumberFormat from "~/components/_molecules/labels/balance-number-format.vue";

defineProps<{
  label: string;
  amount: number;
  currency: Currency;
  icon: string;
  iconColorClass: string;
  amountColorClass?: string;
}>();
</script>

<template>
  <UCard>
    <div class="flex items-center gap-4">
      <div :class="['p-3 rounded-full shrink-0', iconColorClass]">
        <UIcon :name="icon" class="w-6 h-6 flex my-auto" />
      </div>
      <div class="flex flex-col overflow-hidden">
        <span class="text-xs text-neutral-500 dark:text-neutral-400 uppercase tracking-widest font-medium truncate">
          {{ label }}
        </span>
        <span :class="['text-xl font-bold mt-1 truncate', amountColorClass]">
          <BalanceNumberFormat :balance="amount" :currency="currency" format="de-De" />
        </span>
      </div>
    </div>
  </UCard>
</template>
