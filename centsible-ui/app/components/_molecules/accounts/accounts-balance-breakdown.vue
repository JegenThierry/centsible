<script lang="ts" setup>
import type {BudgetAccount} from "~/models/budget-account/budget-account";
import type {Currency} from "~/models/budget-account/currency";
import BalanceNumberFormat from "~/components/_atoms/labels/balance-number-format.vue";

const props = defineProps<{
  accounts: BudgetAccount[];
}>();

const {t} = useI18n();

interface BreakdownRow {
  id: string;
  name: string;
  balance: number;
  currency: Currency;
  widthPct: number;
}

interface CurrencyGroup {
  currency: Currency;
  rows: BreakdownRow[];
}

const groups = computed<CurrencyGroup[]>(() => {
  const byCurrency = new Map<Currency, BudgetAccount[]>();
  for (const account of props.accounts) {
    const list = byCurrency.get(account.currency) ?? [];
    list.push(account);
    byCurrency.set(account.currency, list);
  }

  return [...byCurrency.entries()].map(([currency, list]) => {
    const maxAbs = Math.max(...list.map(a => Math.abs(a.balance)), 1);
    return {
      currency,
      rows: list.map(a => ({
        id: a.id,
        name: a.name,
        balance: a.balance,
        currency: a.currency,
        widthPct: Math.max(2, Math.round((Math.abs(a.balance) / maxAbs) * 100)),
      })),
    };
  });
});
</script>

<template>
  <UCard>
    <template #header>
      <h3 class="text-base font-semibold text-highlighted">{{ t('accounts.breakdown.title') }}</h3>
    </template>

    <div class="space-y-5">
      <div v-for="group in groups" :key="group.currency" class="space-y-2.5">
        <p class="text-xs font-medium text-muted uppercase tracking-wide">{{ group.currency }}</p>
        <div v-for="row in group.rows" :key="row.id" class="space-y-1">
          <div class="flex items-center justify-between gap-3 text-sm">
            <span class="truncate font-medium text-default">{{ row.name }}</span>
            <span :class="['tabular-nums font-semibold shrink-0', row.balance < 0 ? 'text-error' : 'text-highlighted']">
              <BalanceNumberFormat :balance="row.balance" :currency="row.currency"/>
            </span>
          </div>
          <div class="h-2 rounded-full bg-elevated overflow-hidden">
            <div :class="['h-full rounded-full', row.balance < 0 ? 'bg-error' : 'bg-primary']"
                 :style="{width: `${row.widthPct}%`}"/>
          </div>
        </div>
      </div>
    </div>
  </UCard>
</template>
