<script lang="ts" setup>
import type {BudgetAccount} from "~/models/budget-account/budget-account";
import type {Currency} from "~/models/budget-account/currency";
import BalanceNumberFormat from "~/components/_atoms/labels/balance-number-format.vue";

const props = defineProps<{
  accounts: BudgetAccount[];
}>();

const {t} = useI18n();

interface CurrencyTotal {
  currency: Currency;
  balance: number;
}

const totals = computed<CurrencyTotal[]>(() => {
  const byCurrency = new Map<Currency, number>();
  for (const account of props.accounts) {
    byCurrency.set(account.currency, (byCurrency.get(account.currency) ?? 0) + account.balance);
  }
  return [...byCurrency.entries()].map(([currency, balance]) => ({currency, balance}));
});
</script>

<template>
  <UCard>
    <div class="flex flex-wrap items-center gap-x-10 gap-y-4">
      <div class="flex items-center gap-3">
        <div class="p-2.5 rounded-lg bg-primary/10 text-primary flex items-center justify-center">
          <UIcon name="i-lucide-wallet" class="w-5 h-5"/>
        </div>
        <div>
          <p class="text-xs text-muted">{{ t('accounts.overview.accountsLabel') }}</p>
          <p class="text-2xl font-bold tracking-tight tabular-nums">{{ accounts.length }}</p>
        </div>
      </div>

      <div class="hidden sm:block self-stretch border-l border-default"/>

      <div>
        <p class="text-xs text-muted">{{ t('accounts.overview.totalBalanceLabel') }}</p>
        <div class="flex flex-wrap items-baseline gap-x-5 gap-y-1">
          <p v-for="total in totals"
             :key="total.currency"
             :class="[
               'text-2xl font-bold tracking-tight tabular-nums',
               total.balance < 0 ? 'text-error' : 'text-highlighted',
             ]">
            <BalanceNumberFormat :balance="total.balance" :currency="total.currency"/>
          </p>
        </div>
      </div>
    </div>
  </UCard>
</template>
