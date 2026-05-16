<script lang="ts" setup>
import type {Currency} from "~/models/budget-account/currency";

const props = defineProps<{
  currency: Currency;
  balance: number;
  locale?: string;
}>();

const localeTag = useLocaleTag();
const activeLocale = computed(() => props.locale ?? localeTag.value);
</script>

<template>
  <span>
    {{ new Intl.NumberFormat(activeLocale, {style: 'currency', currency: currency}).format(balance) }}
  </span>
</template>

<style scoped>

</style>
