<script lang="ts" setup>
import type {Currency} from "~/models/budget-account/currency";
import type {SetBalanceForm} from "~/models/transactions/transaction";
import BaseInput from "~/components/_atoms/inputs/base-input.vue";
import CategorySelect from "~/components/_atoms/inputs/category-select.vue";
import DateInput from "~/components/_atoms/inputs/date-input.vue";
import {useCategoriesStore} from "~/stores/categoriesStore";
import {BALANCE_INPUT} from "~/utils/money";

const props = defineProps<{
  currentBalance: number;
  currency?: Currency;
  disabled?: boolean;
}>();

const form = defineModel<SetBalanceForm>({required: true});

const {t} = useI18n();
const localeTag = useLocaleTag();
const categoriesStore = useCategoriesStore();

const delta = computed(() => {
  const next = Number(form.value.newBalance);
  if (!Number.isFinite(next)) return 0;
  return Number((next - props.currentBalance).toFixed(2));
});

type DeltaKind = 'income' | 'expense' | 'none';

const deltaKind = computed<DeltaKind>(() => {
  if (delta.value > 0) return 'income';
  if (delta.value < 0) return 'expense';
  return 'none';
});

const previewKey = computed(() => {
  switch (deltaKind.value) {
    case 'income': return 'transactions.form.modes.setBalance.previewIncome';
    case 'expense': return 'transactions.form.modes.setBalance.previewExpense';
    case 'none': return 'transactions.form.modes.setBalance.previewNoChange';
  }
});

const previewText = computed(() => {
  const amount = Math.abs(deltaKind.value === 'none' ? props.currentBalance : delta.value);
  const formatted = props.currency
    ? new Intl.NumberFormat(localeTag.value, {style: 'currency', currency: props.currency}).format(amount)
    : amount.toFixed(2);
  return t(previewKey.value, {amount: formatted});
});

onMounted(() => {
  if (categoriesStore.categories.length === 0) categoriesStore.updateCategories();
});
</script>

<template>
  <div class="space-y-4">
    <BaseInput name="newBalance"
               v-model="form.newBalance"
               :max="BALANCE_INPUT.max"
               :min="BALANCE_INPUT.min"
               :disabled="disabled"
               :label="t('transactions.form.modes.setBalance.newBalance')"
               :placeholder="t('transactions.form.amountPlaceholder')"
               :trailing-text="currency"
               required
               type="number"/>

    <p class="text-sm text-neutral-500">{{ previewText }}</p>

    <CategorySelect name="category"
                    v-model="form.category"
                    :disabled="disabled"
                    :options="categoriesStore.categories"
                    :label="t('transactions.form.category')"
                    required/>

    <BaseInput name="description"
               v-model="form.description"
               :max-length="255"
               :disabled="disabled"
               :label="t('transactions.form.description')"
               :placeholder="t('transactions.form.descriptionPlaceholder')"
               required
               type="text"/>

    <DateInput name="transactionDate"
               v-model="form.transactionDate"
               :disabled="disabled"
               :label="t('transactions.form.date')"
               required/>
  </div>
</template>
