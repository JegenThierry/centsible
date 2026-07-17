<script lang="ts" setup>
import {type Currency, currencyIcon} from "~/models/budget-account/currency";

const props = defineProps<{
  currency: Currency;
}>();

const activeIcon = computed(() => currencyIcon(props.currency));
</script>

<template>
  <div class="p-2 bg-primary-100 dark:bg-primary-900 rounded-lg flex">
    <UIcon :name="activeIcon"
           class="w-6 h-6 text-primary-600 dark:text-primary-400 m-auto"/>
  </div>
</template>
