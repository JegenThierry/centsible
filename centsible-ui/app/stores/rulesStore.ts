import {defineStore} from "pinia";
import adze from 'adze'
import type {Rule, RuleForm} from "~/models/rule/rule";
import {useRuleService} from "~/services/rule/rule-service";
import {useToasts} from "~/services/toasts/toast-service";
import {useApiErrors} from "~/composables/use-api-errors";

export const useRulesStore = defineStore('rulesStore', () => {
  const api = useApi();
  const toasts = useToasts();
  const apiErrors = useApiErrors();
  const {t} = useNuxtApp().$i18n;
  const service = useRuleService(api);

  const rules = ref<Rule[]>([]);
  const pending = ref(false);

  async function updateRules() {
    pending.value = true;
    try {
      rules.value = await service.fetchRules();
    } catch (error) {
      toasts.error(t('rules.toasts.fetchFailedTitle'), t('rules.toasts.fetchFailedBody'));
      adze.ns('rules').error('Failed to fetch rules', error);
    } finally {
      pending.value = false;
    }
  }

  const runMutation = useRunMutation({
    pending,
    refetch: updateRules,
    fallbackBody: () => t('rules.toasts.genericErrorBody'),
    toasts,
    apiErrors,
  });

  async function createRule(form: RuleForm) {
    await runMutation(
      () => service.createRule(form).then(() => undefined),
      t('rules.toasts.createdTitle'), t('rules.toasts.createdBody'), t('rules.toasts.createFailedTitle'),
    );
  }

  async function updateRule(id: string, form: RuleForm) {
    await runMutation(
      () => service.updateRule(id, form).then(() => undefined),
      t('rules.toasts.updatedTitle'), t('rules.toasts.updatedBody'), t('rules.toasts.updateFailedTitle'),
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
      apiErrors.toastError(error, t('rules.toasts.applyFailedTitle'), t('rules.toasts.genericErrorBody'));
      throw error;
    } finally {
      pending.value = false;
    }
  }

  return {rules, pending, updateRules, createRule, updateRule, deleteRule, applyRule};
});
