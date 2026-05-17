<script lang="ts" setup>
import {type Category, CategoryType} from "~/models/category/category";
import {type TransactionForm} from "~/models/transactions/transaction";
import type {Currency} from "~/models/budget-account/currency";
import BaseInput from "~/components/_atoms/inputs/base-input.vue";
import CategorySelect from "~/components/_atoms/inputs/category-select.vue";
import CategoryTypeBadge from "~/components/_molecules/badges/category-type-badge.vue";
import DateInput from "~/components/_atoms/inputs/date-input.vue";
import {useCategoryService} from "~/services/category/category-service";

const props = defineProps<{
  modelValue: TransactionForm;
  filterType?: CategoryType;
  currency?: Currency;
  disabled?: boolean;
}>();

const emit = defineEmits(['update:modelValue']);

const api = useApi();
const categoryService = useCategoryService(api);
const categories = ref<Category[]>([]);
const {t} = useI18n();

const amountInput = ref<InstanceType<typeof BaseInput>>();
const descriptionInput = ref<InstanceType<typeof BaseInput>>();
const categoryInput = ref();
const dateInput = ref();

async function loadCategories() {
  try {
    const all = await categoryService.fetchCategories();
    categories.value = props.filterType ? all.filter(c => c.type === props.filterType) : all;
  } catch (error) {
    console.error('Failed to load categories:', error);
  }
}

const form = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
});

onMounted(loadCategories);

defineExpose({
  validate: () => useValidator().validateInputs([amountInput, descriptionInput, categoryInput, dateInput]),
});
</script>

<template>
  <div class="space-y-4">
    <CategorySelect ref="categoryInput"
                    v-model="form.category"
                    :disabled="disabled"
                    :options="categories"
                    :label="t('transactions.form.category')"
                    required/>

    <div v-if="form.category" class="flex items-center gap-2 text-sm">
      <span class="text-neutral-500">{{ t('transactions.form.transactionType') }}</span>
      <CategoryTypeBadge :type="form.category.type"/>
    </div>

    <BaseInput ref="amountInput"
               v-model="form.amount"
               :max="9999999.99"
               :min="0.01"
               :disabled="disabled"
               :label="t('transactions.form.amount')"
               :placeholder="t('transactions.form.amountPlaceholder')"
               :trailing-text="currency"
               required
               type="number"/>

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
