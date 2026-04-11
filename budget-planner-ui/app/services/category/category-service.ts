import type {AxiosInstance} from "axios";
import {validateRequest} from "~/composables/use-api";
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
        const response = await api.delete(`/categories/${id}`);
        if (response.status !== 200 && response.status !== 204) {
            throw new Error(response.statusText);
        }
    }

    return {
        fetchCategories,
        createCategory,
        updateCategory,
        deleteCategory,
    }
}
