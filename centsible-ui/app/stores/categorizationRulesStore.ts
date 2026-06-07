import {defineStore} from "pinia";
import adze from 'adze'
import type {CategorizationRule, CategorizationRuleForm} from "~/models/categorization/rule";
import {useCategorizationService} from "~/services/categorization/categorization-service";
import {useToasts} from "~/services/toasts/toast-service";
import {useApiErrors} from "~/composables/use-api-errors";

export const useCategorizationRulesStore = defineStore('categorizationRulesStore', () => {
  const api = useApi();
  const toasts = useToasts();
  const apiErrors = useApiErrors();
  const {t} = useNuxtApp().$i18n;
  const service = useCategorizationService(api);

  const rules = ref<CategorizationRule[]>([]);
  const pending = ref(false);

  async function updateRules() {
    pending.value = true;
    try {
      rules.value = await service.fetchRules();
    } catch (error) {
      toasts.error(t('categories.rules.toasts.fetchFailedTitle'), t('categories.rules.toasts.fetchFailedBody'));
      adze.ns('categorization').error('Failed to fetch rules', error);
    } finally {
      pending.value = false;
    }
  }

  async function runMutation(action: () => Promise<void>, successTitle: string, successBody: string, errorTitle: string) {
    pending.value = true;
    try {
      await action();
      await updateRules();
      toasts.success(successTitle, successBody);
    } catch (error) {
      apiErrors.toastError(error, errorTitle, t('categories.rules.toasts.genericErrorBody'));
      throw error;
    } finally {
      pending.value = false;
    }
  }

  async function createRule(form: CategorizationRuleForm) {
    await runMutation(
      () => service.createRule(form).then(() => undefined),
      t('categories.rules.toasts.createdTitle'), t('categories.rules.toasts.createdBody'), t('categories.rules.toasts.createFailedTitle'),
    );
  }

  async function updateRule(id: string, form: CategorizationRuleForm) {
    await runMutation(
      () => service.updateRule(id, form).then(() => undefined),
      t('categories.rules.toasts.updatedTitle'), t('categories.rules.toasts.updatedBody'), t('categories.rules.toasts.updateFailedTitle'),
    );
  }

  async function deleteRule(id: string) {
    await service.deleteRule(id);
    await updateRules();
  }

  async function applyRule(id: string): Promise<number> {
    pending.value = true;
    try {
      const result = await service.applyRule(id);
      await updateRules();
      return result.updated;
    } catch (error) {
      apiErrors.toastError(error, t('categories.rules.toasts.applyFailedTitle'), t('categories.rules.toasts.genericErrorBody'));
      throw error;
    } finally {
      pending.value = false;
    }
  }

  return {rules, pending, updateRules, createRule, updateRule, deleteRule, applyRule};
});
