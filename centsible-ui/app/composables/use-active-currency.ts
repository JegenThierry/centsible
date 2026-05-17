import {Currency} from "~/models/budget-account/currency";
import {useBudgetAccountsStore} from "~/stores/budgetAccountsStore";

export function useActiveCurrency() {
  const store = useBudgetAccountsStore();
  return computed(() => store.activeAccount?.currency ?? Currency.EUR);
}
