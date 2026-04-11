<script setup lang="ts">
import {useCategoriesStore} from "~/stores/categoriesStore";
import type {Category} from "~/models/category/category";
import CancelButton from "~/components/_molecules/buttons/cancel-button.vue";
import DeleteButton from "~/components/_molecules/buttons/delete-button.vue";

const props = defineProps<{
  category?: Category;
}>();

const isOpen = defineModel<boolean>('open', {required: true});

const categoriesStore = useCategoriesStore();
const loading = ref(false);

async function handleDelete() {
  if (!props.category?.id) return;

  loading.value = true;
  try {
    await categoriesStore.deleteCategory(props.category.id);
    isOpen.value = false;
  } catch (error) {
    console.error('Failed to delete category:', error);
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <UModal v-model:open="isOpen"
          title="Delete Category"
          :description="`Are you sure you want to delete the category '${category?.name}'? This action cannot be undone.`">
    <template #footer>
      <div class="flex justify-end gap-2">
        <CancelButton @click="isOpen = false"/>
        <DeleteButton :loading="loading" @click="handleDelete"/>
      </div>
    </template>
  </UModal>
</template>
