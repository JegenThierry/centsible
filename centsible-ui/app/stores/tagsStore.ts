import {defineStore} from 'pinia';
import adze from 'adze';
import {useTagService} from '~/services/tag/tag-service';
import type {Tag, TagForm} from '~/models/tag/tag';

export const useTagsStore = defineStore('tagsStore', () => {
  const service = useTagService(useApi());

  const tags = ref<Tag[]>([]);
  const loading = ref(false);

  async function fetchAll() {
    loading.value = true;
    try {
      tags.value = await service.fetchAll();
    } catch (error) {
      adze.ns('tags').error('Failed to load tags', error);
      tags.value = [];
    } finally {
      loading.value = false;
    }
  }

  /** Throws on failure so callers can surface a localized toast (e.g. duplicate name). */
  async function create(form: TagForm): Promise<Tag> {
    const created = await service.create(form);
    tags.value = [...tags.value, created].sort((a, b) => a.name.localeCompare(b.name));
    return created;
  }

  async function update(id: number, form: TagForm): Promise<Tag> {
    const updated = await service.update(id, form);
    tags.value = tags.value.map(t => (t.id === id ? updated : t)).sort((a, b) => a.name.localeCompare(b.name));
    return updated;
  }

  async function remove(id: number): Promise<boolean> {
    try {
      await service.remove(id);
      tags.value = tags.value.filter(t => t.id !== id);
      return true;
    } catch (error) {
      adze.ns('tags').error('Failed to delete tag', error);
      return false;
    }
  }

  return {tags, loading, fetchAll, create, update, remove};
});
