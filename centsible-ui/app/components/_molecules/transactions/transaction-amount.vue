<script lang="ts" setup>
import {CategoryType} from "~/models/category/category";
import type {Currency} from "~/models/budget-account/currency";
import BalanceNumberFormat from "~/components/_atoms/labels/balance-number-format.vue";

const props = defineProps<{
  amount: number;
  type?: CategoryType;
  currency: Currency;
}>();

const {t} = useI18n();

const isIncome = computed(() => props.type === CategoryType.INCOME);
const typeLabel = computed(() =>
  props.type ? t(`transactions.category.type.${props.type}`) : undefined,
);
</script>

<template>
  <div :class="isIncome ? 'text-success' : 'text-error'"
       class="inline-flex items-center justify-end gap-1 text-sm font-semibold">
    <UIcon :name="isIncome ? 'i-lucide-arrow-up' : 'i-lucide-arrow-down'"
           :aria-label="typeLabel"
           class="w-3.5 h-3.5 shrink-0"/>
    <BalanceNumberFormat :balance="amount" :currency="currency"/>
  </div>
</template>
