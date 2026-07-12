<script lang="ts" setup>
import type {BudgetAccount} from "~/models/budget-account/budget-account";
import type {Currency} from "~/models/budget-account/currency";
import BalanceNumberFormat from "~/components/_atoms/labels/balance-number-format.vue";
import BalanceChangeBadge from "~/components/_molecules/badges/balance-change-badge.vue";

const props = withDefaults(defineProps<{
  accounts: BudgetAccount[];
  previousBalances?: Record<string, number>;
}>(), {
  previousBalances: () => ({}),
});

const {t} = useI18n();

interface CurrencyTotal {
  currency: Currency;
  balance: number;
  previousBalance: number;
}

const totals = computed<CurrencyTotal[]>(() => {
  const byCurrency = new Map<Currency, CurrencyTotal>();
  for (const account of props.accounts) {
    const entry = byCurrency.get(account.currency)
      ?? {currency: account.currency, balance: 0, previousBalance: 0};
    const previous = props.previousBalances[account.id] ?? account.initialBalance;
    entry.balance += account.balance;
    entry.previousBalance += previous;
    byCurrency.set(account.currency, entry);
  }
  return [...byCurrency.values()];
});

const trendsLoaded = computed(() => Object.keys(props.previousBalances).length > 0);
</script>

<template>
  <UCard>
    <div class="flex items-center justify-between gap-3 mb-4">
      <p class="text-xs font-medium uppercase tracking-widest text-muted">
        {{ t('accounts.overview.totalBalanceLabel') }}
      </p>
      <div class="flex items-center gap-1.5 text-muted">
        <UIcon name="i-lucide-wallet" class="w-4 h-4"/>
        <span class="text-sm tabular-nums">{{ accounts.length }}</span>
        <span class="text-sm">{{ t('accounts.overview.accountsLabel') }}</span>
      </div>
    </div>

    <div class="flex flex-wrap gap-3">
      <div v-for="total in totals"
           :key="total.currency"
           class="flex-1 min-w-[12rem] max-w-xs rounded-lg border border-default px-4 py-3">
        <p class="text-xs font-medium uppercase tracking-wide text-muted">{{ total.currency }}</p>
        <p :class="[
             'mt-0.5 text-2xl font-bold tracking-tight tabular-nums',
             total.balance < 0 ? 'text-error' : 'text-highlighted',
           ]">
          <BalanceNumberFormat :balance="total.balance" :currency="total.currency"/>
        </p>
        <div v-if="trendsLoaded" class="mt-1.5 flex items-center gap-2">
          <BalanceChangeBadge :currency="total.currency"
                              :current-balance="total.balance"
                              :previous-balance="total.previousBalance"/>
          <span class="text-xs text-muted">{{ t('accounts.overview.last30Days') }}</span>
        </div>
      </div>
    </div>
  </UCard>
</template>
