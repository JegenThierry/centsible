<script lang="ts" setup>
import type {Currency} from "~/models/budget-account/currency";
import BalanceNumberFormat from "~/components/_atoms/labels/balance-number-format.vue";

const props = defineProps<{
  currentBalance: number;
  previousBalance: number;
  currency: Currency;
}>();

const balanceChange = computed(() => props.currentBalance - props.previousBalance);

const tone = computed(() => {
  if (balanceChange.value > 0) return {color: 'success' as const, icon: 'i-lucide-trending-up'};
  if (balanceChange.value < 0) return {color: 'error' as const, icon: 'i-lucide-trending-down'};
  return {color: 'neutral' as const, icon: 'i-lucide-minus'};
});
</script>

<template>
  <UBadge :color="tone.color"
          class="ml-1 mt-0.5 tabular-nums font-semibold"
          size="sm"
          variant="subtle">
    <UIcon :name="tone.icon" class="mr-1 size-3.5"/>
    <BalanceNumberFormat :balance="balanceChange" :currency="currency"/>
  </UBadge>
</template>
