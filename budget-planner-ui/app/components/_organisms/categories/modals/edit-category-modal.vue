<script lang="ts" setup>
import {useCategoriesStore} from "~/stores/categoriesStore";
import {type Category, type CategoryForm, CategoryType} from "~/models/category/category";
import BaseInput from "~/components/_atoms/inputs/base-input.vue";
import IconInput from "~/components/_molecules/inputs/icon-input.vue";
import ColorSelect from "~/components/_atoms/inputs/color-select.vue";
import CancelButton from "~/components/_molecules/buttons/cancel-button.vue";
import {useValidator} from "~/composables/use-validator";

const props = defineProps<{
  category?: Category;
}>();

const isOpen = defineModel<boolean>('open', {required: true});

const categoriesStore = useCategoriesStore();

const form = ref<CategoryForm>({
  name: '',
  icon: 'i-lucide-tag',
  color: '#3b82f6',
  type: CategoryType.EXPENSE,
});

const nameInput = ref<InstanceType<typeof BaseInput>>();
const iconInput = ref<InstanceType<typeof IconInput>>();

const loading = ref(false);

const typeOptions = [
  {label: 'Expense', value: CategoryType.EXPENSE},
  {label: 'Income', value: CategoryType.INCOME},
];

watch(() => props.category, (newCategory) => {
  if (newCategory) {
    form.value = {
      name: newCategory.name,
      icon: newCategory.icon,
      color: newCategory.color,
      type: newCategory.type,
    };
  }
}, {immediate: true});

async function handleSave() {
  if (!props.category?.id) return;

  const inputs = [nameInput, iconInput];
  if (!useValidator().validateInputs(inputs)) {
    return;
  }

  loading.value = true;
  try {
    await categoriesStore.updateCategory(props.category.id, form.value);
    isOpen.value = false;
  } catch (error) {
    console.error('Failed to update category:', error);
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <UModal v-model:open="isOpen"
          description="Update your category details."
          title="Edit Category">
    <template #body>
      <div class="space-y-4">
        <URadioGroup v-model="form.type"
                     :items="typeOptions"
                     legend="Category Type"
                     orientation="horizontal"/>

        <BaseInput ref="nameInput"
                   v-model="form.name"
                   label="Name"
                   placeholder="e.g. Food, Salary"
                   required
                   type="text"/>

        <IconInput ref="iconInput"
                   v-model="form.icon"
                   required/>

        <ColorSelect v-model="form.color"
                     label="Color"
                     required/>
      </div>
    </template>

    <template #footer>
      <div class="flex justify-end gap-2">
        <CancelButton @click="isOpen = false"/>
        <UButton :loading="loading" @click="handleSave">Save Changes</UButton>
      </div>
    </template>
  </UModal>
</template>
