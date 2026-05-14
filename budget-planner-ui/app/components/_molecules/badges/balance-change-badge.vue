<script lang="ts" setup>
import type {Currency} from "~/models/budget-account/currency";
import BalanceNumberFormat from "~/components/_molecules/labels/balance-number-format.vue";

const props = defineProps<{
  currentBalance: number;
  previousBalance: number;
  currency: Currency;
}>();

const balanceChange = computed(() => props.currentBalance - props.previousBalance);

const badgeColor = computed(() => {
  if (balanceChange.value > 0) return 'success';
  if (balanceChange.value < 0) return 'error';
  return 'neutral';
});

const badgeIcon = computed(() => {
  if (balanceChange.value > 0) return 'i-lucide-trending-up';
  if (balanceChange.value < 0) return 'i-lucide-trending-down';
  return 'i-lucide-minus';
});
</script>

<template>
  <UBadge :color="badgeColor"
          class="ml-1 mt-0.5 font-mono font-semibold"
          size="sm"
          variant="subtle">

    <UIcon :name="badgeIcon"
           class="mr-1 size-3.5"/>

    <BalanceNumberFormat :balance="balanceChange"
                         :currency="currency"
                         format="de-De"/>
  </UBadge>
</template>

<style scoped>

</style>
