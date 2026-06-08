import type {CategoryType} from "~/models/category/category";
import type {Rule, RuleCondition} from "~/models/rule/rule";

export interface RuleContext {
  description: string;
  amount: number;
  type: CategoryType;
  accountId: string;
}

export interface RuleEffects {
  categoryId?: number;
  tagIds: number[];
  categoryRuleName?: string;
}

export function conditionMatches(condition: RuleCondition, context: RuleContext): boolean {
  switch (condition.field) {
    case 'DESCRIPTION': {
      const value = condition.value.toLowerCase();
      const description = context.description.toLowerCase();
      switch (condition.operator) {
        case 'CONTAINS': return description.includes(value);
        case 'EQUALS': return description === value;
        case 'STARTS_WITH': return description.startsWith(value);
        default: return false;
      }
    }
    case 'AMOUNT': {
      if (condition.value.trim() === '') return false;
      const target = Number(condition.value);
      if (Number.isNaN(target)) return false;
      switch (condition.operator) {
        case 'GT': return context.amount > target;
        case 'GTE': return context.amount >= target;
        case 'LT': return context.amount < target;
        case 'LTE': return context.amount <= target;
        case 'EQUALS': return context.amount === target;
        default: return false;
      }
    }
    case 'DIRECTION':
      return condition.operator === 'IS' && context.type.toLowerCase() === condition.value.toLowerCase();
    case 'ACCOUNT':
      return condition.operator === 'IS' && context.accountId.toLowerCase() === condition.value.toLowerCase();
    default:
      return false;
  }
}

export function ruleMatches(rule: Rule, context: RuleContext): boolean {
  if (rule.conditions.length === 0) return false;
  return rule.matchAll
    ? rule.conditions.every((c) => conditionMatches(c, context))
    : rule.conditions.some((c) => conditionMatches(c, context));
}

export function evaluateRules(rules: Rule[], context: RuleContext): RuleEffects {
  let categoryId: number | undefined;
  let categoryRuleName: string | undefined;
  const tagIds: number[] = [];
  for (const rule of rules) {
    if (!rule.enabled || !ruleMatches(rule, context)) continue;
    for (const action of rule.actions) {
      if (action.type === 'SET_CATEGORY') {
        const category = action.category;
        if (categoryId === undefined && category?.id != null && category.type === context.type) {
          categoryId = category.id;
          categoryRuleName = rule.name;
        }
      } else if (action.type === 'ADD_TAG') {
        const tagId = action.tag?.id;
        if (tagId != null && !tagIds.includes(tagId)) tagIds.push(tagId);
      }
    }
  }
  return {categoryId, tagIds, categoryRuleName};
}
