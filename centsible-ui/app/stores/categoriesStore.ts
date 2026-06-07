import {defineStore} from "pinia";
import adze from 'adze'
import type {Category, CategoryForm} from "~/models/category/category";
import {useCategoryService} from "~/services/category/category-service";
import {useToasts} from "~/services/toasts/toast-service";
import {useApiErrors} from "~/composables/use-api-errors";

export const useCategoriesStore = defineStore('categoriesStore', () => {
  const api = useApi();
  const toasts = useToasts();
  const apiErrors = useApiErrors();
  const {t} = useNuxtApp().$i18n;
  const categoryService = useCategoryService(api);

  const categories = ref<Category[]>([]);
  const pending = ref(false);

  async function updateCategories() {
    pending.value = true;
    try {
      categories.value = await categoryService.fetchCategories();
    } catch (error) {
      toasts.error(t('categories.toasts.fetchFailedTitle'), t('categories.toasts.fetchFailedBody'));
      adze.ns('categories').error('Failed to fetch categories', error);
    } finally {
      pending.value = false;
    }
  }

  async function runMutation(action: () => Promise<void>, successTitle: string, successBody: string, errorTitle: string) {
    pending.value = true;
    try {
      await action();
      await updateCategories();
      toasts.success(successTitle, successBody);
    } catch (error) {
      apiErrors.toastError(error, errorTitle, t('categories.toasts.genericErrorBody'));
      throw error;
    } finally {
      pending.value = false;
    }
  }

  async function createCategory(form: CategoryForm) {
    await runMutation(
      () => categoryService.createCategory(form).then(() => undefined),
      t('categories.toasts.createdTitle'),
      t('categories.toasts.createdBody'),
      t('categories.toasts.createFailedTitle'),
    );
  }

  async function updateCategory(id: number, form: CategoryForm) {
    await runMutation(
      () => categoryService.updateCategory(id, form).then(() => undefined),
      t('categories.toasts.updatedTitle'),
      t('categories.toasts.updatedBody'),
      t('categories.toasts.updateFailedTitle'),
    );
  }

  async function deleteCategory(id: number) {
    await runMutation(
      () => categoryService.deleteCategory(id),
      t('categories.toasts.deletedTitle'),
      t('categories.toasts.deletedBody'),
      t('categories.toasts.deleteFailedTitle'),
    );
  }

  return {
    categories,
    pending,
    updateCategories,
    createCategory,
    updateCategory,
    deleteCategory,
  }
});
