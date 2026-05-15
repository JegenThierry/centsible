<script lang="ts" setup>
import {CategoryType} from "~/models/category/category";
import type {Currency} from "~/models/budget-account/currency";
import BalanceNumberFormat from "~/components/_molecules/labels/balance-number-format.vue";

defineProps<{
  amount: number;
  type?: CategoryType;
  currency: Currency;
}>();
</script>

<template>
  <div :class="type === CategoryType.INCOME ? 'text-green-600 dark:text-green-400' : 'text-red-600 dark:text-red-400'"
       class="text-sm font-semibold">
    <BalanceNumberFormat :balance="amount" :currency="currency"/>
  </div>
</template>
