<script lang="ts" setup>
import {useCategoriesStore} from "~/stores/categoriesStore";
import type {Category} from "~/models/category/category";
import ModalFooterActions from "~/components/_molecules/modals/modal-footer-actions.vue";

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
      <ModalFooterActions :loading="loading"
                          :submit-label="t('common.actions.delete')"
                          submit-color="error"
                          @cancel="isOpen = false"
                          @submit="handleDelete"/>
    </template>
  </UModal>
</template>
