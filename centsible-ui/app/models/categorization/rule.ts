import type {Category} from "~/models/category/category";

export type MatchType = 'CONTAINS' | 'EQUALS' | 'STARTS_WITH';

export const MATCH_TYPES: MatchType[] = ['CONTAINS', 'EQUALS', 'STARTS_WITH'];

export interface CategorizationRule {
  id: string;
  matchType: MatchType;
  pattern: string;
  category: Category;
  priority: number;
  createdAt: string;
  updatedAt: string;
}

export interface CategorizationRuleForm {
  matchType: MatchType;
  pattern: string;
  categoryId: number;
  priority: number;
}
