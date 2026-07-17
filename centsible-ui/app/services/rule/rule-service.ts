import type {AxiosInstance} from "axios";
import {crudResource, validateRequest} from "~/composables/use-api";
import type {Rule, RuleForm} from "~/models/rule/rule";

export interface ApplyRuleResult {
  updated: number;
}

export function useRuleService(api: AxiosInstance) {
  const resource = crudResource<Rule, string, RuleForm>(api, '/rules');

  /** Re-evaluates rule [id] against the user's existing transactions; returns rows touched. */
  async function applyRule(id: string): Promise<ApplyRuleResult> {
    return validateRequest<ApplyRuleResult>(await api.post<ApplyRuleResult>(`/rules/${id}/apply`, {}));
  }

  return {
    fetchRules: (): Promise<Rule[]> => resource.list(),
    createRule: (form: RuleForm): Promise<Rule> => resource.create(form),
    updateRule: (id: string, form: RuleForm): Promise<Rule> => resource.update(id, form),
    deleteRule: (id: string): Promise<void> => resource.remove(id),
    applyRule,
  };
}
