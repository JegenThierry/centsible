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
  <div :class="type === CategoryType.INCOME ? 'text-success' : 'text-error'"
       class="text-sm font-semibold">
    <BalanceNumberFormat :balance="amount" :currency="currency"/>
  </div>
</template>
