import type {Category} from "~/models/category/category";
import type {Tag} from "~/models/tag/tag";

export type RuleField = 'DESCRIPTION' | 'AMOUNT' | 'DIRECTION' | 'ACCOUNT';
export type RuleOperator = 'CONTAINS' | 'EQUALS' | 'STARTS_WITH' | 'GT' | 'GTE' | 'LT' | 'LTE' | 'IS';
export type RuleActionType = 'SET_CATEGORY' | 'ADD_TAG';

export const RULE_FIELDS: RuleField[] = ['DESCRIPTION', 'AMOUNT', 'DIRECTION', 'ACCOUNT'];
export const RULE_ACTION_TYPES: RuleActionType[] = ['SET_CATEGORY', 'ADD_TAG'];
export const DIRECTIONS = ['INCOME', 'EXPENSE'] as const;

export const OPERATORS_BY_FIELD: Record<RuleField, RuleOperator[]> = {
  DESCRIPTION: ['CONTAINS', 'EQUALS', 'STARTS_WITH'],
  AMOUNT: ['GT', 'GTE', 'LT', 'LTE', 'EQUALS'],
  DIRECTION: ['IS'],
  ACCOUNT: ['IS'],
};

export interface RuleCondition {
  id?: string;
  field: RuleField;
  operator: RuleOperator;
  value: string;
}

export interface RuleAction {
  id?: string;
  type: RuleActionType;
  category?: Category | null;
  tag?: Tag | null;
}

export interface Rule {
  id: string;
  name: string;
  matchAll: boolean;
  enabled: boolean;
  priority: number;
  conditions: RuleCondition[];
  actions: RuleAction[];
  createdAt: string;
  updatedAt: string;
}

export interface RuleConditionForm {
  field: RuleField;
  operator: RuleOperator;
  value: string;
}

export interface RuleActionForm {
  type: RuleActionType;
  categoryId?: number | null;
  tagId?: number | null;
}

export interface RuleForm {
  name: string;
  matchAll: boolean;
  enabled: boolean;
  priority: number;
  conditions: RuleConditionForm[];
  actions: RuleActionForm[];
}
