<script lang="ts" setup>
import {type Category, CategoryType} from "~/models/category/category";
import type {BudgetForm} from "~/models/budget/budget";
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
    console.error('Failed to load categories:', error);
  }
}

const form = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val),
});

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
  </div>
</template>
