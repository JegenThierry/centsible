<script lang="ts" setup>
import BalanceNumberFormat from "~/components/_atoms/labels/balance-number-format.vue";
import type {BudgetVsActualPeriod} from "~/models/reports/budget-vs-actual";
import type {Currency} from "~/models/budget-account/currency";

const props = defineProps<{
  periods: BudgetVsActualPeriod[];
  currency: Currency;
}>();

const {t} = useI18n();

function adherencePct(spent: number, limit: number): number {
  if (limit <= 0) return 0;
  return Math.min(100, Math.round((spent / limit) * 100));
}

function adherenceColor(spent: number, limit: number): 'neutral' | 'error' | 'warning' | 'success' {
  if (limit <= 0) return 'neutral';
  const ratio = spent / limit;
  if (ratio >= 1) return 'error';
  if (ratio >= 0.85) return 'warning';
  return 'success';
}
</script>

<template>
  <UCard>
    <template #header>
      <div class="flex flex-col gap-0.5">
        <h3 class="text-base font-semibold text-highlighted">{{ t('reports.budgetVsActual.title') }}</h3>
        <p class="text-xs text-muted">
          {{ t('reports.budgetVsActual.description', {count: periods.length}) }}
        </p>
      </div>
    </template>

    <div v-if="periods.length === 0 || periods.every(p => p.entries.length === 0)"
         class="text-sm text-muted py-4 text-center">
      {{ t('reports.budgetVsActual.empty') }}
    </div>

    <div v-else class="space-y-5">
      <section v-for="period in periods" :key="period.periodKey">
        <p class="text-sm font-semibold mb-2">{{ period.periodKey }}</p>

        <div v-if="period.entries.length === 0" class="text-xs text-muted">
          {{ t('reports.budgetVsActual.noBudgets') }}
        </div>

        <ul v-else class="space-y-2">
          <li v-for="entry in period.entries" :key="entry.categoryId" class="text-sm">
            <div class="flex items-center justify-between mb-1">
              <span class="flex items-center gap-2 truncate">
                <span class="w-2 h-2 rounded-full shrink-0"
                      :style="{backgroundColor: entry.categoryColor ?? '#a3a3a3'}"/>
                <span class="truncate">{{ entry.categoryName }}</span>
              </span>
              <span class="tabular-nums text-xs text-muted shrink-0">
                <BalanceNumberFormat :balance="Number(entry.spent)" :currency="currency"/>
                /
                <BalanceNumberFormat :balance="Number(entry.limit)" :currency="currency"/>
              </span>
            </div>
            <UProgress :color="adherenceColor(Number(entry.spent), Number(entry.limit))"
                       :model-value="adherencePct(Number(entry.spent), Number(entry.limit))"
                       :ui="{base: 'h-1.5 bg-elevated/60'}"
                       size="sm"/>
          </li>
        </ul>
      </section>
    </div>
  </UCard>
</template>
