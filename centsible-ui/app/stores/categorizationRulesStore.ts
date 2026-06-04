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
  const service = useCategorizationService(api);

  const rules = ref<CategorizationRule[]>([]);
  const pending = ref(false);

  async function updateRules() {
    pending.value = true;
    try {
      rules.value = await service.fetchRules();
    } catch (error) {
      toasts.error("Failed to fetch rules", "Categorization rules could not be loaded");
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
      apiErrors.toastError(error, errorTitle, "An error occurred");
      throw error;
    } finally {
      pending.value = false;
    }
  }

  async function createRule(form: CategorizationRuleForm) {
    await runMutation(
      () => service.createRule(form).then(() => undefined),
      "Rule created", "The categorization rule has been added", "Failed to create rule",
    );
  }

  async function updateRule(id: string, form: CategorizationRuleForm) {
    await runMutation(
      () => service.updateRule(id, form).then(() => undefined),
      "Rule updated", "The categorization rule has been saved", "Failed to update rule",
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
      apiErrors.toastError(error, "Failed to apply rule", "An error occurred");
      throw error;
    } finally {
      pending.value = false;
    }
  }

  return {rules, pending, updateRules, createRule, updateRule, deleteRule, applyRule};
});
