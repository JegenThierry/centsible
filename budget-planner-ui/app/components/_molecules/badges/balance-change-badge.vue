<script lang="ts" setup>
import type {Currency} from "~/models/budget-account/currency";
import BalanceNumberFormat from "~/components/_molecules/labels/balance-number-format.vue";

const props = defineProps<{
  currentBalance: number;
  previousBalance: number;
  currency: Currency;
}>();

const balanceChange = computed(() => props.currentBalance - props.previousBalance);
</script>

<template>
  <UBadge :color="balanceChange > 0 ? 'success' : 'error'"
          class="ml-1 mt-0.5 font-mono font-semibold"
          size="sm"
          variant="subtle">

    <UIcon :name="balanceChange > 0 ? 'i-lucide-trending-up' : 'i-lucide-trending-down'"
           class="mr-1 size-3.5"/>

    <BalanceNumberFormat :balance="balanceChange"
                         :currency="currency"
                         format="de-De"/>
  </UBadge>
</template>

<style scoped>

</style>
