import {defineStore} from "pinia";
import type {Category, CategoryForm} from "~/models/category/category";
import {useCategoryService} from "~/services/category/category-service";
import {useToasts} from "~/services/toasts/toast-service";
import {useApiErrors} from "~/composables/use-api-errors";

export const useCategoriesStore = defineStore('categoriesStore', () => {
  const api = useApi();
  const toasts = useToasts();
  const apiErrors = useApiErrors();
  const categoryService = useCategoryService(api);

  const categories = ref<Category[]>([]);
  const pending = ref(false);

  async function updateCategories() {
    pending.value = true;
    try {
      categories.value = await categoryService.fetchCategories();
    } catch (error) {
      toasts.error("Failed to fetch categories", "Categories could not be loaded");
      console.error(error);
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
      apiErrors.toastError(error, errorTitle, "An error occurred");
      throw error;
    } finally {
      pending.value = false;
    }
  }

  async function createCategory(form: CategoryForm) {
    await runMutation(
      () => categoryService.createCategory(form).then(() => undefined),
      "Category created",
      "New category has been added",
      "Failed to create category",
    );
  }

  async function updateCategory(id: number, form: CategoryForm) {
    await runMutation(
      () => categoryService.updateCategory(id, form).then(() => undefined),
      "Category updated",
      "Category has been updated",
      "Failed to update category",
    );
  }

  async function deleteCategory(id: number) {
    await runMutation(
      () => categoryService.deleteCategory(id),
      "Category deleted",
      "Category has been removed",
      "Failed to delete category",
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
