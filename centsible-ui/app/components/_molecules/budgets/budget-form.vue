<script lang="ts" setup>
import adze from 'adze'
import {type Category, CategoryType} from "~/models/category/category";
import {BUDGET_PERIOD_TYPES, type BudgetForm, type BudgetPeriodType} from "~/models/budget/budget";
import BaseInput from "~/components/_atoms/inputs/base-input.vue";
import CategorySelect from "~/components/_atoms/inputs/category-select.vue";
import AppSelect from "~/components/_atoms/ui/app-select.vue";
import AppCheckbox from "~/components/_atoms/ui/app-checkbox.vue";
import {useCategoryService} from "~/services/category/category-service";

const props = defineProps<{
  modelValue: BudgetForm;
  /** (category, period) pairs already budgeted — excluded from the picker to avoid a duplicate. */
  existingCombos?: Array<{categoryId: number; periodType: BudgetPeriodType}>;
}>();

const emit = defineEmits(['update:modelValue']);

const api = useApi();
const categoryService = useCategoryService(api);
const categories = ref<Category[]>([]);
const {t} = useI18n();

async function loadCategories() {
  try {
    const all = await categoryService.fetchCategories();
    categories.value = all.filter(c => c.type === CategoryType.EXPENSE);
  } catch (error) {
    adze.ns('budgets').error('Failed to load categories', error);
  }
}

const form = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val),
});

const periodOptions = computed(() =>
  BUDGET_PERIOD_TYPES.map((value) => ({value, label: t(`budgets.periods.${value}`)})),
);

const excludedCategoryIds = computed(() => {
  const selectedId = form.value.category?.id;
  return new Set(
    (props.existingCombos ?? [])
      .filter(c => c.periodType === form.value.periodType && c.categoryId !== selectedId)
      .map(c => c.categoryId),
  );
});

const availableCategories = computed(() =>
  categories.value.filter(c => !excludedCategoryIds.value.has(c.id)),
);

watch(() => form.value.periodType, (period) => {
  const id = form.value.category?.id;
  if (id === undefined) return;
  const collides = (props.existingCombos ?? []).some(c => c.periodType === period && c.categoryId === id);
  if (collides) form.value.category = undefined;
});

onMounted(() => loadCategories());
</script>

<template>
  <div class="space-y-4">
    <CategorySelect name="category"
                    v-model="form.category"
                    :options="availableCategories"
                    :label="t('budgets.form.categoryLabel')"
                    required/>

    <BaseInput name="amountLimit"
               v-model="form.amountLimit"
               :max="9999999.99"
               :min="0.01"
               :description="t('budgets.form.limitDescription')"
               :label="t('budgets.form.limitLabel')"
               :placeholder="t('budgets.form.limitPlaceholder')"
               required
               type="number"/>

    <div>
      <label class="text-xs font-medium text-neutral-600 dark:text-neutral-300">
        {{ t('budgets.form.periodLabel') }}
      </label>
      <AppSelect v-model="form.periodType" :items="periodOptions" class="w-full mt-1"/>
    </div>

    <AppCheckbox v-model="form.rolloverEnabled"
               :label="t('budgets.form.rolloverLabel')"
               :description="t('budgets.form.rolloverDescription')"/>
  </div>
</template>
