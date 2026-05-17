<script lang="ts" setup>
import type {Currency} from "~/models/budget-account/currency";

const props = defineProps<{
  currency: Currency;
  balance: number;
  locale?: string;
}>();

const localeTag = useLocaleTag();
const formatted = computed(() => new Intl.NumberFormat(
  props.locale ?? localeTag.value,
  {style: 'currency', currency: props.currency},
).format(props.balance));
</script>

<template>
  <span>{{ formatted }}</span>
</template>
