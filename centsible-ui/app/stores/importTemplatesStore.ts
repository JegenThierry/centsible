import {defineStore} from 'pinia';
import adze from 'adze';
import {useImportService} from '~/services/imports/import-service';
import {upsertSortedByName} from '~/utils/upsert';
import type {ImportMappingTemplate, ImportMappingTemplateForm} from '~/models/imports/imports';

/**
 * User-saved CSV import profiles. Backs the "saved profiles" picker in the import wizard so a
 * recurring statement from the same bank can be re-imported without re-doing the column mapping.
 */
export const useImportTemplatesStore = defineStore('importTemplatesStore', () => {
  const service = useImportService(useApi());

  const templates = ref<ImportMappingTemplate[]>([]);
  const loading = ref(false);

  async function fetchAll() {
    loading.value = true;
    try {
      templates.value = await service.listTemplates();
    } catch (error) {
      adze.ns('imports').error('Failed to load import profiles', error);
      templates.value = [];
    } finally {
      loading.value = false;
    }
  }

  /** Throws on failure so the caller can surface a localized toast (e.g. duplicate name). */
  async function create(form: ImportMappingTemplateForm): Promise<ImportMappingTemplate> {
    loading.value = true;
    try {
      const created = await service.createTemplate(form);
      upsertSortedByName(templates, created);
      return created;
    } finally {
      loading.value = false;
    }
  }

  async function remove(id: string): Promise<boolean> {
    try {
      await service.deleteTemplate(id);
      templates.value = templates.value.filter(tpl => tpl.id !== id);
      return true;
    } catch (error) {
      adze.ns('imports').error('Failed to delete import profile', error);
      return false;
    }
  }

  return {templates, loading, fetchAll, create, remove};
});
