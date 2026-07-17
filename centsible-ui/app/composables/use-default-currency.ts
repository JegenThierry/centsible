import {Currency} from "~/models/budget-account/currency";
import {useUserStore} from "~/stores/userStore";

export function useDefaultCurrency() {
  const store = useUserStore();
  return computed(() => store.user?.defaultCurrency ?? Currency.EUR);
}
