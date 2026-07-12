<script lang="ts" setup>
import type {BudgetAccount} from "~/models/budget-account/budget-account";
import type {Currency} from "~/models/budget-account/currency";
import BalanceNumberFormat from "~/components/_atoms/labels/balance-number-format.vue";
import {accountNetContribution} from "~/models/budget-account/account-type";

const props = defineProps<{
  accounts: BudgetAccount[];
}>();

const {t} = useI18n();

interface Segment {
  id: string;
  name: string;
  balance: number;
  sharePct: number;
  negative: boolean;
  opacity: number;
}

interface CurrencyGroup {
  currency: Currency;
  total: number;
  segments: Segment[];
}

const groups = computed<CurrencyGroup[]>(() => {
  const byCurrency = new Map<Currency, BudgetAccount[]>();
  for (const account of props.accounts) {
    const list = byCurrency.get(account.currency) ?? [];
    list.push(account);
    byCurrency.set(account.currency, list);
  }

  return [...byCurrency.entries()].map(([currency, list]) => {
    const valued = list.map(account => ({account, value: accountNetContribution(account)}));
    const gross = Math.max(valued.reduce((sum, v) => sum + Math.abs(v.value), 0), 1);
    const net = valued.reduce((sum, v) => sum + v.value, 0);
    const sorted = [...valued].sort((a, b) => Math.abs(b.value) - Math.abs(a.value));
    const positiveCount = sorted.filter(v => v.value >= 0).length;
    const step = positiveCount > 1 ? 0.55 / (positiveCount - 1) : 0;

    let positiveIndex = 0;
    const segments = sorted.map<Segment>(({account, value}) => {
      const negative = value < 0;
      const opacity = negative ? 1 : 1 - positiveIndex * step;
      if (!negative) positiveIndex++;
      return {
        id: account.id,
        name: account.name,
        balance: value,
        sharePct: Math.round((Math.abs(value) / gross) * 100),
        negative,
        opacity,
      };
    });

    return {currency, total: net, segments};
  });
});
</script>

<template>
  <UCard>
    <template #header>
      <h3 class="text-base font-semibold text-highlighted">{{ t('accounts.breakdown.title') }}</h3>
    </template>

    <div class="space-y-6">
      <div v-for="group in groups" :key="group.currency" class="space-y-3">
        <div class="flex items-baseline justify-between gap-3">
          <p class="text-xs font-medium uppercase tracking-wide text-muted">{{ group.currency }}</p>
          <span :class="['text-sm font-semibold tabular-nums', group.total < 0 ? 'text-error' : 'text-highlighted']">
            <BalanceNumberFormat :balance="group.total" :currency="group.currency"/>
          </span>
        </div>

        <div class="flex gap-1 h-2.5 overflow-hidden">
          <div v-for="seg in group.segments"
               :key="seg.id"
               :class="['h-full rounded-full', seg.negative ? 'bg-error' : 'bg-primary']"
               :style="{width: `${Math.max(seg.sharePct, 2)}%`, opacity: seg.opacity}"/>
        </div>

        <div class="space-y-1.5">
          <div v-for="seg in group.segments" :key="seg.id" class="flex items-center gap-2 text-sm">
            <span :class="['w-2.5 h-2.5 rounded-full shrink-0', seg.negative ? 'bg-error' : 'bg-primary']"
                  :style="{opacity: seg.opacity}"/>
            <span class="truncate font-medium text-default">{{ seg.name }}</span>
            <span class="ml-auto tabular-nums text-muted shrink-0">{{ seg.sharePct }}%</span>
            <span :class="['w-24 text-right tabular-nums font-semibold shrink-0', seg.balance < 0 ? 'text-error' : 'text-highlighted']">
              <BalanceNumberFormat :balance="seg.balance" :currency="group.currency"/>
            </span>
          </div>
        </div>
      </div>
    </div>
  </UCard>
</template>
