<script lang="ts" setup>
import type {Currency} from "~/models/budget-account/currency";

const props = defineProps<{
  currency: Currency;
  balance: number;
  locale?: string;
}>();

const localeTag = useLocaleTag();
const formatted = computed(() => formatCurrency(props.balance, props.currency, props.locale ?? localeTag.value));
</script>

<template>
  <span>{{ formatted }}</span>
</template>
