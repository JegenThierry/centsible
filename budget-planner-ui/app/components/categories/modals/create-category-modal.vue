<script setup lang="ts">
import {useCategoriesStore} from "~/stores/categoriesStore";
import {CategoryType, type CategoryForm} from "~/models/category/category";
import BaseInput from "~/components/_atoms/inputs/base-input.vue";
import IconInput from "~/components/_molecules/inputs/icon-input.vue";
import CancelButton from "~/components/_molecules/buttons/cancel-button.vue";
import {useValidator} from "~/composables/use-validator";

const isOpen = defineModel<boolean>('open', {required: true});

const categoriesStore = useCategoriesStore();

const form = ref<CategoryForm>({
  name: '',
  icon: 'i-lucide-tag',
  type: CategoryType.EXPENSE,
});

const nameInput = ref<InstanceType<typeof BaseInput>>();
const iconInput = ref<InstanceType<typeof IconInput>>();

const loading = ref(false);

const typeOptions = [
  {label: 'Expense', value: CategoryType.EXPENSE},
  {label: 'Income', value: CategoryType.INCOME},
];

function resetForm() {
  form.value = {
    name: '',
    icon: 'i-lucide-tag',
    type: CategoryType.EXPENSE,
  };
}

watch(isOpen, (newValue) => {
  if (newValue) {
    resetForm();
  }
});

async function handleSave() {
  const inputs = [nameInput, iconInput];
  if (!useValidator().validateInputs(inputs)) {
    return;
  }

  loading.value = true;
  try {
    await categoriesStore.createCategory(form.value);
    isOpen.value = false;
  } catch (error) {
    console.error('Failed to create category:', error);
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <UModal v-model:open="isOpen"
          title="Create Category"
          description="Create a new category for your transactions.">
    <template #body>
      <div class="space-y-4">
        <URadioGroup v-model="form.type"
                     :items="typeOptions"
                     legend="Category Type"
                     orientation="horizontal"/>

        <BaseInput ref="nameInput"
                   v-model="form.name"
                   label="Name"
                   type="text"
                   required
                   placeholder="e.g. Food, Salary"/>

        <IconInput ref="iconInput"
                   v-model="form.icon"
                   required/>
      </div>
    </template>

    <template #footer>
      <div class="flex justify-end gap-2">
        <CancelButton @click="isOpen = false"/>
        <UButton :loading="loading" @click="handleSave">Create</UButton>
      </div>
    </template>
  </UModal>
</template>
