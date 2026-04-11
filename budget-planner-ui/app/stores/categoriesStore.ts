import {defineStore} from "pinia";
import type {Category, CategoryForm} from "~/models/category/category";
import {useCategoryService} from "~/services/category/category-service";
import {useToasts} from "~/services/toasts/toast-service";

export const useCategoriesStore = defineStore('categoriesStore', () => {
  const api = useApi();
  const toasts = useToasts();
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

  async function createCategory(form: CategoryForm) {
    pending.value = true;
    try {
      await categoryService.createCategory(form);
      await updateCategories();
      toasts.success("Category created", "New category has been added");
    } catch (error: any) {
      toasts.error("Failed to create category", error.response?.data || "An error occurred");
      throw error;
    } finally {
      pending.value = false;
    }
  }

  async function updateCategory(id: number, form: CategoryForm) {
    pending.value = true;
    try {
      await categoryService.updateCategory(id, form);
      await updateCategories();
      toasts.success("Category updated", "Category has been updated");
    } catch (error: any) {
      toasts.error("Failed to update category", error.response?.data || "An error occurred");
      throw error;
    } finally {
      pending.value = false;
    }
  }

  async function deleteCategory(id: number) {
    pending.value = true;
    try {
      await categoryService.deleteCategory(id);
      await updateCategories();
      toasts.success("Category deleted", "Category has been removed");
    } catch (error: any) {
      toasts.error("Failed to delete category", error.response?.data || "An error occurred");
      throw error;
    } finally {
      pending.value = false;
    }
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
