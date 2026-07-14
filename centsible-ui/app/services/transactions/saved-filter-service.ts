import type {AxiosInstance} from "axios";
import type {SavedFilter, SavedFilterForm} from "~/models/transactions/saved-filter";
import {assertStatus, validateRequest} from "~/composables/use-api";

/** Stateless client for the saved-filter-view CRUD endpoints (owner-scoped on the server). */
export function useSavedFilterService(api: AxiosInstance) {
  async function fetchAll(): Promise<SavedFilter[]> {
    const response = await api.get<SavedFilter[]>('/saved-filters');
    return validateRequest<SavedFilter[]>(response);
  }

  async function create(form: SavedFilterForm): Promise<SavedFilter> {
    const response = await api.post<SavedFilter>('/saved-filters', form);
    return validateRequest<SavedFilter>(response);
  }

  async function remove(id: number): Promise<void> {
    assertStatus(await api.delete(`/saved-filters/${id}`));
  }

  return {fetchAll, create, remove};
}
