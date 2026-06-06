<script lang="ts" setup>
import {type Category, CategoryType} from "~/models/category/category";
import {type TransactionForm} from "~/models/transactions/transaction";
import type {Currency} from "~/models/budget-account/currency";
import BaseInput from "~/components/_atoms/inputs/base-input.vue";
import CategorySelect from "~/components/_atoms/inputs/category-select.vue";
import CurrencySelect from "~/components/_atoms/inputs/currency-select.vue";
import DateInput from "~/components/_atoms/inputs/date-input.vue";
import AppRadioGroup from "~/components/_atoms/ui/app-radio-group.vue";
import BalanceNumberFormat from "~/components/_atoms/labels/balance-number-format.vue";
import {useCategoriesStore} from "~/stores/categoriesStore";
import {useConversionPreview} from "~/composables/use-conversion-preview";
import {AMOUNT_INPUT} from "~/utils/money";

const props = defineProps<{
  filterType?: CategoryType;
  disabled?: boolean;
  accountId?: string;
  accountCurrency?: Currency;
}>();

const form = defineModel<TransactionForm>({required: true});

const {t} = useI18n();

const {converted: previewAmount, failed: previewFailed, isForeign: previewIsForeign} = useConversionPreview({
  accountId: computed(() => props.accountId),
  accountCurrency: computed(() => props.accountCurrency),
  amount: computed(() => form.value.amount),
  currency: computed(() => form.value.currency),
  date: computed(() => form.value.transactionDate),
});
const categoriesStore = useCategoriesStore();

const amountInput = ref<InstanceType<typeof BaseInput>>();
const descriptionInput = ref<InstanceType<typeof BaseInput>>();
const categoryInput = ref<InstanceType<typeof CategorySelect>>();
const dateInput = ref<InstanceType<typeof DateInput>>();

// Once the user picks a type explicitly, picking a new category must not
// silently overwrite their choice.
const userTouchedType = ref(false);

const visibleCategories = computed(() => {
  const all = categoriesStore.categories;
  return props.filterType ? all.filter(c => c.type === props.filterType) : all;
});

watch(
  () => props.filterType,
  (filter) => {
    userTouchedType.value = false;
    if (filter) form.value.type = filter;
  },
  {immediate: true},
);

function onCategoryPicked(cat: Category | undefined) {
  if (cat && !userTouchedType.value) form.value.type = cat.type;
}

const typeOptions = computed(() => [
  {label: t('transactions.form.typeIncome'), value: CategoryType.INCOME},
  {label: t('transactions.form.typeExpense'), value: CategoryType.EXPENSE},
]);

function onTypeChange(value: CategoryType) {
  form.value.type = value;
  userTouchedType.value = true;
}

onMounted(() => {
  if (categoriesStore.categories.length === 0) categoriesStore.updateCategories();
});

defineExpose({
  validate: () => useValidator().validateInputs([amountInput, descriptionInput, categoryInput, dateInput]),
});
</script>

<template>
  <div class="space-y-4">
    <CategorySelect ref="categoryInput"
                    v-model="form.category"
                    :disabled="disabled"
                    :options="visibleCategories"
                    :label="t('transactions.form.category')"
                    required
                    @update:model-value="onCategoryPicked"/>

    <div v-if="!filterType" class="flex flex-col gap-1">
      <span class="text-sm text-neutral-500">{{ t('transactions.form.typeLabel') }}</span>
      <AppRadioGroup :model-value="form.type"
                   :disabled="disabled"
                   :items="typeOptions"
                   orientation="horizontal"
                   @update:model-value="onTypeChange"/>
    </div>

    <UFormField :label="t('transactions.form.currency')" name="currency">
      <CurrencySelect v-model="form.currency"
                      :disabled="disabled"
                      :placeholder="t('transactions.form.currencyPlaceholder')"/>
    </UFormField>

    <BaseInput ref="amountInput"
               v-model="form.amount"
               :max="AMOUNT_INPUT.max"
               :min="AMOUNT_INPUT.min"
               :disabled="disabled"
               :label="t('transactions.form.amount')"
               :placeholder="t('transactions.form.amountPlaceholder')"
               :trailing-text="form.currency"
               required
               type="number"/>

    <p v-if="previewIsForeign && form.amount > 0" class="-mt-2 px-1 text-xs text-neutral-400">
      <span v-if="previewAmount != null && accountCurrency">
        ≈ <BalanceNumberFormat :balance="previewAmount" :currency="accountCurrency"/>
      </span>
      <span v-else-if="previewFailed">{{ t('transactions.form.conversionUnavailable') }}</span>
      <span v-else>≈ …</span>
    </p>

    <BaseInput ref="descriptionInput"
               v-model="form.description"
               :max-length="255"
               :disabled="disabled"
               :label="t('transactions.form.description')"
               :placeholder="t('transactions.form.descriptionPlaceholder')"
               required
               type="text"/>

    <DateInput ref="dateInput"
               v-model="form.transactionDate"
               :disabled="disabled"
               :label="t('transactions.form.date')"
               required/>
  </div>
</template>
