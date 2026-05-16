<script lang="ts" setup>
import {useCategoriesStore} from "~/stores/categoriesStore";
import type {Category} from "~/models/category/category";
import CancelButton from "~/components/_molecules/buttons/cancel-button.vue";
import DeleteButton from "~/components/_molecules/buttons/delete-button.vue";

const props = defineProps<{
  category?: Category;
}>();

const isOpen = defineModel<boolean>('open', {required: true});

const categoriesStore = useCategoriesStore();
const {t} = useI18n();
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
          :description="t('categories.delete.description', {name: category?.name ?? ''})"
          :title="t('categories.delete.title')">
    <template #footer>
      <div class="flex justify-end gap-2">
        <CancelButton @click="isOpen = false"/>
        <DeleteButton :loading="loading" @click="handleDelete"/>
      </div>
    </template>
  </UModal>
</template>
