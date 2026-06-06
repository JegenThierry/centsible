import type {AxiosInstance} from "axios";
import {assertStatus, validateRequest} from "~/composables/use-api";
import type {Category, CategoryForm} from "~/models/category/category";

export function useCategoryService(api: AxiosInstance) {
  async function fetchCategories(): Promise<Category[]> {
    const response = await api.get<Category[]>('/categories');
    return validateRequest<Category[]>(response);
  }

  async function createCategory(category: CategoryForm): Promise<Category> {
    const response = await api.post<Category>('/categories', category);
    return validateRequest<Category>(response);
  }

  async function updateCategory(id: number, category: CategoryForm): Promise<Category> {
    const response = await api.put<Category>(`/categories/${id}`, category);
    return validateRequest<Category>(response);
  }

  async function deleteCategory(id: number): Promise<void> {
    assertStatus(await api.delete(`/categories/${id}`));
  }

  return {
    fetchCategories,
    createCategory,
    updateCategory,
    deleteCategory,
  }
}
