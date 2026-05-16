<script lang="ts" setup>
import {type Category} from "~/models/category/category";
import {type RecurringTransactionForm} from "~/models/recurring/recurring-transaction";
import BaseInput from "~/components/_atoms/inputs/base-input.vue";
import CategorySelect from "~/components/_atoms/inputs/category-select.vue";
import DateInput from "~/components/_atoms/inputs/date-input.vue";
import FrequencySelect from "~/components/_atoms/inputs/frequency-select.vue";
import CategoryTypeBadge from "~/components/_molecules/badges/category-type-badge.vue";
import {useCategoryService} from "~/services/category/category-service";

const props = defineProps<{
  modelValue: RecurringTransactionForm;
}>();

const emit = defineEmits(['update:modelValue']);

const api = useApi();
const categoryService = useCategoryService(api);
const categories = ref<Category[]>([]);
const {t} = useI18n();

const amountInput = ref<InstanceType<typeof BaseInput>>();
const descriptionInput = ref<InstanceType<typeof BaseInput>>();
const categoryInput = ref();
const frequencyInput = ref();
const startDateInput = ref();

async function loadCategories() {
  try {
    categories.value = await categoryService.fetchCategories();
  } catch (error) {
    console.error('Failed to load categories:', error);
  }
}

const form = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val),
});

onMounted(() => {
  loadCategories();
});

defineExpose({
  validate: () => useValidator().validateInputs([
    amountInput,
    descriptionInput,
    categoryInput,
    frequencyInput,
    startDateInput,
  ]),
});
</script>

<template>
  <div class="space-y-4">
    <CategorySelect ref="categoryInput"
                    v-model="form.category"
                    :options="categories"
                    :label="t('transactions.recurring.form.category')"
                    required/>

    <div v-if="form.category" class="flex items-center gap-2 text-sm">
      <span class="text-neutral-500">{{ t('transactions.recurring.form.transactionType') }}</span>
      <CategoryTypeBadge :type="form.category.type"/>
    </div>

    <BaseInput ref="amountInput"
               v-model="form.amount"
               :max="9999999.99"
               :min="0.01"
               :label="t('transactions.recurring.form.amount')"
               :placeholder="t('transactions.recurring.form.amountPlaceholder')"
               required
               type="number"/>

    <BaseInput ref="descriptionInput"
               v-model="form.description"
               :max-length="255"
               :label="t('transactions.recurring.form.description')"
               :placeholder="t('transactions.recurring.form.descriptionPlaceholder')"
               required
               type="text"/>

    <FrequencySelect ref="frequencyInput"
                     v-model="form.frequency"
                     :label="t('transactions.recurring.form.frequency')"
                     required/>

    <DateInput ref="startDateInput"
               v-model="form.startDate"
               :description="t('transactions.recurring.form.startDateHelp')"
               :label="t('transactions.recurring.form.startDate')"
               required/>

    <DateInput v-model="form.endDate"
               :description="t('transactions.recurring.form.endDateHelp')"
               :label="t('transactions.recurring.form.endDate')"/>
  </div>
</template>
