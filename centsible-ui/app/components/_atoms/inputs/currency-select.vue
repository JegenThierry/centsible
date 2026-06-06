<script lang="ts" setup>
import {Currency, currencyOptions} from "~/models/budget-account/currency";
import AppSelect from "~/components/_atoms/ui/app-select.vue";

const model = defineModel<Currency>({required: true});
defineProps<{
  disabled?: boolean;
  placeholder?: string;
}>();

const activeIcon = computed(() => currencyOptions.find(item => item.value === model.value)?.icon);
</script>

<template>
  <AppSelect v-model="model"
             :icon="activeIcon"
             :items="currencyOptions"
             :disabled="disabled"
             :placeholder="placeholder"
             class="w-full"/>
</template>
