<script setup lang="ts">
import type {Currency} from "~/models/budget-account/currency";

withDefaults(
  defineProps<{
    currency: Currency;
    balance: number;
    format: 'de-De' | 'en-US'
  }>(), {
    format: 'de-De'
  }
);
</script>

<template>
  <span>
    {{ new Intl.NumberFormat(format, {style: 'currency', currency: currency}).format(balance) }}
  </span>
</template>

<style scoped>

</style>
