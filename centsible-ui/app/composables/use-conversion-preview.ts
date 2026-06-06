import {watchDebounced} from "@vueuse/core";
import type {Ref} from "vue";
import type {Currency} from "~/models/budget-account/currency";
import {useTransactionService} from "~/services/transactions/transaction-service";

export function useConversionPreview(params: {
  accountId: Ref<string | undefined>;
  accountCurrency: Ref<Currency | undefined>;
  amount: Ref<number | undefined>;
  currency: Ref<Currency | undefined>;
  date: Ref<string | undefined>;
}) {
  const service = useTransactionService(useApi());

  const converted = ref<number | null>(null);
  const loading = ref(false);
  const failed = ref(false);
  let token = 0;

  const isForeign = computed(() =>
    !!params.accountCurrency.value
    && !!params.currency.value
    && params.currency.value !== params.accountCurrency.value,
  );

  watchDebounced(
    [params.accountId, params.accountCurrency, params.amount, params.currency, params.date],
    async () => {
      const current = ++token;
      converted.value = null;
      failed.value = false;

      const amount = params.amount.value;
      const accountId = params.accountId.value;
      const currency = params.currency.value;
      if (!isForeign.value || !accountId || !currency || !amount || amount <= 0) {
        loading.value = false;
        return;
      }

      loading.value = true;
      try {
        const result = await service.previewConversion(accountId, amount, currency, params.date.value);
        if (current === token) converted.value = result.convertedAmount;
      } catch {
        if (current === token) failed.value = true;
      } finally {
        if (current === token) loading.value = false;
      }
    },
    {debounce: 400, immediate: true},
  );

  return {converted, loading, failed, isForeign};
}
