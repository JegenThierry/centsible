import type {AxiosInstance} from "axios";
import {crudResource} from "~/composables/use-api";
import type {Category, CategoryForm} from "~/models/category/category";

export function useCategoryService(api: AxiosInstance) {
  const resource = crudResource<Category, number, CategoryForm>(api, '/categories');

  return {
    fetchCategories: (): Promise<Category[]> => resource.list(),
    createCategory: (category: CategoryForm): Promise<Category> => resource.create(category),
    updateCategory: (id: number, category: CategoryForm): Promise<Category> => resource.update(id, category),
    deleteCategory: (id: number): Promise<void> => resource.remove(id),
  }
}
