<script lang="ts" setup>
import type {Currency} from "~/models/budget-account/currency";

const props = defineProps<{
  currency: Currency;
  balance: number;
  format?: string;
}>();

const locale = computed(() => {
  if (props.format) return props.format;
  if (typeof navigator !== 'undefined' && navigator.language) return navigator.language;
  return 'en-US';
});
</script>

<template>
  <span>
    {{ new Intl.NumberFormat(locale, {style: 'currency', currency: currency}).format(balance) }}
  </span>
</template>

<style scoped>

</style>
