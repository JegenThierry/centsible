<script lang="ts" setup>
import adze from 'adze'
import {type Category, CategoryType} from "~/models/category/category";
import {BUDGET_PERIOD_TYPES, type BudgetForm} from "~/models/budget/budget";
import BaseInput from "~/components/_atoms/inputs/base-input.vue";
import CategorySelect from "~/components/_atoms/inputs/category-select.vue";
import {useCategoryService} from "~/services/category/category-service";

const props = defineProps<{
  modelValue: BudgetForm;
}>();

const emit = defineEmits(['update:modelValue']);

const api = useApi();
const categoryService = useCategoryService(api);
const categories = ref<Category[]>([]);
const {t} = useI18n();

const limitInput = ref<InstanceType<typeof BaseInput>>();
const categoryInput = ref();

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

onMounted(() => loadCategories());

defineExpose({
  validate: () => useValidator().validateInputs([categoryInput, limitInput]),
});
</script>

<template>
  <div class="space-y-4">
    <CategorySelect ref="categoryInput"
                    v-model="form.category"
                    :options="categories"
                    :label="t('budgets.form.categoryLabel')"
                    required/>

    <BaseInput ref="limitInput"
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
      <USelect v-model="form.periodType" :items="periodOptions" class="w-full mt-1"/>
    </div>

    <UCheckbox v-model="form.rolloverEnabled"
               :label="t('budgets.form.rolloverLabel')"
               :description="t('budgets.form.rolloverDescription')"/>
  </div>
</template>
