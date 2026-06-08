import type {AxiosInstance} from "axios";
import {assertStatus, validateRequest} from "~/composables/use-api";
import type {Rule, RuleForm} from "~/models/rule/rule";

export interface ApplyRuleResult {
  updated: number;
}

export function useRuleService(api: AxiosInstance) {
  async function fetchRules(): Promise<Rule[]> {
    return validateRequest<Rule[]>(await api.get<Rule[]>('/rules'));
  }

  async function createRule(form: RuleForm): Promise<Rule> {
    return validateRequest<Rule>(await api.post<Rule>('/rules', form));
  }

  async function updateRule(id: string, form: RuleForm): Promise<Rule> {
    return validateRequest<Rule>(await api.put<Rule>(`/rules/${id}`, form));
  }

  async function deleteRule(id: string): Promise<void> {
    assertStatus(await api.delete(`/rules/${id}`));
  }

  /** Re-evaluates rule [id] against the user's existing transactions; returns rows touched. */
  async function applyRule(id: string): Promise<ApplyRuleResult> {
    return validateRequest<ApplyRuleResult>(await api.post<ApplyRuleResult>(`/rules/${id}/apply`, {}));
  }

  return {fetchRules, createRule, updateRule, deleteRule, applyRule};
}
