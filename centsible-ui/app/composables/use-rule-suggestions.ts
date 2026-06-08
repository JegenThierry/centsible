import type {Ref} from "vue";
import type {CategoryType} from "~/models/category/category";
import type {TransactionForm} from "~/models/transactions/transaction";
import {evaluateRules, type RuleContext} from "~/models/rule/rule-matching";
import {useRulesStore} from "~/stores/rulesStore";
import {useCategoriesStore} from "~/stores/categoriesStore";

interface DraftBaseline {
  description: string;
  amount: number;
  type: CategoryType;
}

export function useRuleSuggestions(opts: {
  enabled: Ref<boolean>;
  form: Ref<TransactionForm>;
  accountId: Ref<string | undefined>;
}) {
  const {enabled, form, accountId} = opts;
  const rulesStore = useRulesStore();
  const categoriesStore = useCategoriesStore();

  const appliedRule = ref<string | null>(null);

  let categoryTouched = false;
  let managedTags = new Set<number>();
  let baseline: DraftBaseline | null = null;

  function markCategoryTouched() {
    categoryTouched = true;
    appliedRule.value = null;
  }

  function reset() {
    categoryTouched = false;
    managedTags = new Set<number>();
    appliedRule.value = null;
    baseline = {description: form.value.description, amount: form.value.amount, type: form.value.type};
    if (rulesStore.rules.length === 0) rulesStore.updateRules();
  }

  function untouchedDraft(): boolean {
    if (!baseline) return true;
    return baseline.description === form.value.description
      && baseline.amount === form.value.amount
      && baseline.type === form.value.type;
  }

  function applyCategory(categoryId: number | undefined) {
    if (categoryTouched || form.value.splits) return;
    if (categoryId === undefined || form.value.category?.id === categoryId) return;
    const category = categoriesStore.categories.find((c) => c.id === categoryId);
    if (category) form.value.category = category;
  }

  function applyTags(suggested: number[]) {
    const want = new Set(suggested);
    const next = [...(form.value.tagIds ?? [])];
    const stillManaged = new Set<number>();
    let changed = false;

    for (const id of managedTags) {
      if (!want.has(id)) {
        const i = next.indexOf(id);
        if (i >= 0) {
          next.splice(i, 1);
          changed = true;
        }
      }
    }
    for (const id of want) {
      if (managedTags.has(id)) {
        if (next.includes(id)) stillManaged.add(id);
      } else if (!next.includes(id)) {
        next.push(id);
        stillManaged.add(id);
        changed = true;
      }
    }

    managedTags = stillManaged;
    if (changed) form.value.tagIds = next;
  }

  watch(
    [
      enabled,
      accountId,
      () => form.value.description,
      () => form.value.amount,
      () => form.value.type,
      () => rulesStore.rules,
    ],
    () => {
      if (!enabled.value || !accountId.value || untouchedDraft()) return;
      const context: RuleContext = {
        description: form.value.description,
        amount: Math.abs(Number(form.value.amount) || 0),
        type: form.value.type,
        accountId: accountId.value,
      };
      const effects = evaluateRules(rulesStore.rules, context);
      applyCategory(effects.categoryId);
      applyTags(effects.tagIds);
      appliedRule.value = categoryTouched || form.value.splits || effects.categoryId === undefined
        ? null
        : effects.categoryRuleName ?? null;
    },
  );

  return {reset, markCategoryTouched, appliedRule};
}
