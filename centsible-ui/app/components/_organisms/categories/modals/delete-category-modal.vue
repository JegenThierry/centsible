<script lang="ts" setup>
import ConfirmationModal from "~/components/_organisms/modals/confirmation-modal.vue";
import {useCategoriesStore} from "~/stores/categoriesStore";
import type {Category} from "~/models/category/category";

const props = defineProps<{
  category?: Category;
}>();

const isOpen = defineModel<boolean>('open', {required: true});

const categoriesStore = useCategoriesStore();
const {t} = useI18n();

async function deleteCategory() {
  if (!props.category?.id) return;
  await categoriesStore.deleteCategory(props.category.id);
}
</script>

<template>
  <ConfirmationModal v-model:open="isOpen"
                     :title="t('categories.delete.title')"
                     :body="t('categories.delete.description', {name: category?.name ?? ''})"
                     :manage-toasts="false"
                     :delete-callback="deleteCategory"/>
</template>
