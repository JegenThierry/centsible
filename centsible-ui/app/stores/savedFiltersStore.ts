import {defineStore} from 'pinia'
import adze from 'adze'
import {useSavedFilterService} from "~/services/transactions/saved-filter-service";
import type {SavedFilter, SavedFilterForm} from "~/models/transactions/saved-filter";

export const useSavedFiltersStore = defineStore('savedFiltersStore', () => {
  const service = useSavedFilterService(useApi());
  const savedFilters = ref<SavedFilter[]>([]);
  const loading = ref(false);
  const loaded = ref(false);

  async function refresh() {
    loading.value = true;
    try {
      savedFilters.value = await service.fetchAll();
      loaded.value = true;
    } catch (error) {
      adze.ns('savedFilters').error('Failed to load saved filters', error);
    } finally {
      loading.value = false;
    }
  }

  /** Loads the list once; cheap no-op on subsequent calls. */
  async function ensureLoaded() {
    if (!loaded.value) await refresh();
  }

  /** Creates a view and keeps the list name-sorted. Errors bubble so the caller can toast them. */
  async function create(form: SavedFilterForm): Promise<SavedFilter> {
    loading.value = true;
    try {
      const created = await service.create(form);
      savedFilters.value = [...savedFilters.value, created].sort((a, b) => a.name.localeCompare(b.name));
      return created;
    } finally {
      loading.value = false;
    }
  }

  async function remove(id: number): Promise<void> {
    await service.remove(id);
    savedFilters.value = savedFilters.value.filter((f) => f.id !== id);
  }

  return {savedFilters, loading, loaded, refresh, ensureLoaded, create, remove};
});
