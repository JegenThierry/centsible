import type {AxiosInstance} from "axios";
import {assertStatus, validateRequest} from "~/composables/use-api";
import type {CategorizationRule, CategorizationRuleForm} from "~/models/categorization/rule";

export interface ApplyRuleResult {
  updated: number;
}

export function useCategorizationService(api: AxiosInstance) {
  async function fetchRules(): Promise<CategorizationRule[]> {
    return validateRequest<CategorizationRule[]>(await api.get<CategorizationRule[]>('/categorization-rules'));
  }

  async function createRule(form: CategorizationRuleForm): Promise<CategorizationRule> {
    return validateRequest<CategorizationRule>(await api.post<CategorizationRule>('/categorization-rules', form));
  }

  async function updateRule(id: string, form: CategorizationRuleForm): Promise<CategorizationRule> {
    return validateRequest<CategorizationRule>(await api.put<CategorizationRule>(`/categorization-rules/${id}`, form));
  }

  async function deleteRule(id: string): Promise<void> {
    assertStatus(await api.delete(`/categorization-rules/${id}`));
  }

  async function applyRule(id: string): Promise<ApplyRuleResult> {
    return validateRequest<ApplyRuleResult>(await api.post<ApplyRuleResult>(`/categorization-rules/${id}/apply`, {}));
  }

  return {fetchRules, createRule, updateRule, deleteRule, applyRule};
}
