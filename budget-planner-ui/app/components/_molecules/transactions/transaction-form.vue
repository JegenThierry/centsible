<script setup lang="ts">
import { CategoryType, type Category } from "~/models/category/category";
import { type TransactionForm } from "~/models/transactions/transaction";
import BaseInput from "~/components/_atoms/inputs/base-input.vue";
import CategorySelect from "~/components/_atoms/inputs/category-select.vue";
import CategoryTypeBadge from "~/components/_molecules/badges/category-type-badge.vue";
import DateInput from "~/components/_atoms/inputs/date-input.vue";
import { useCategoryService } from "~/services/category/category-service";

const props = defineProps<{
  modelValue: TransactionForm;
  filterType?: CategoryType;
}>();

const emit = defineEmits(['update:modelValue']);

const api = useApi();
const categoryService = useCategoryService(api);
const categories = ref<Category[]>([]);

const amountInput = ref<InstanceType<typeof BaseInput>>();
const descriptionInput = ref<InstanceType<typeof BaseInput>>();
const categoryInput = ref();
const dateInput = ref();

async function loadCategories() {
  try {
    const allCategories = await categoryService.fetchCategories();
    if (props.filterType) {
      categories.value = allCategories.filter(c => c.type === props.filterType);
    } else {
      categories.value = allCategories;
    }
  } catch (error) {
    console.error('Failed to load categories:', error);
  }
}

const form = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
});

onMounted(() => {
  loadCategories();
});

defineExpose({
  validate: () => {
    const inputs = [
      amountInput,
      descriptionInput,
      categoryInput,
      dateInput
    ];
    return useValidator().validateInputs(inputs);
  }
});
</script>

<template>
  <div class="space-y-4">
    <CategorySelect ref="categoryInput"
                    v-model="form.category"
                    label="Category"
                    :options="categories"
                    required />

    <div v-if="form.category" class="flex items-center gap-2 text-sm">
      <span class="text-neutral-500">Transaction Type:</span>
      <CategoryTypeBadge :type="form.category.type" />
    </div>

    <BaseInput ref="amountInput"
               v-model="form.amount"
               label="Amount"
               type="number"
               required
               placeholder="0.00" />

    <BaseInput ref="descriptionInput"
               v-model="form.description"
               label="Description"
               type="text"
               required
               placeholder="Lunch, Groceries, etc." />

    <DateInput ref="dateInput"
               v-model="form.transactionDate"
               label="Date"
               required />
  </div>
</template>
