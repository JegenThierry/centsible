<script setup lang="ts">
import {type Currency, currencyOptions} from "~/models/budget-account/currency";

const props = defineProps<{
  currency: Currency;
}>();

const currencyIcon = computed(() => currencyOptions.find(c => c.value === props.currency)?.icon);
</script>

<template>
  <div class="p-2 bg-primary-100 dark:bg-primary-900 rounded-lg flex">
    <UIcon :name="currencyIcon"
           class="w-6 h-6 text-primary-600 dark:text-primary-400 m-auto"/>
  </div>
</template>
