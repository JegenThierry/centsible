import type {AxiosInstance} from "axios";
import type {SavedFilter, SavedFilterForm} from "~/models/transactions/saved-filter";
import {crudResource} from "~/composables/use-api";

/** Stateless client for the saved-filter-view CRUD endpoints (owner-scoped on the server). */
export function useSavedFilterService(api: AxiosInstance) {
  const resource = crudResource<SavedFilter, number, SavedFilterForm>(api, '/saved-filters');

  return {
    fetchAll: (): Promise<SavedFilter[]> => resource.list(),
    create: (form: SavedFilterForm): Promise<SavedFilter> => resource.create(form),
    remove: (id: number): Promise<void> => resource.remove(id),
  };
}
