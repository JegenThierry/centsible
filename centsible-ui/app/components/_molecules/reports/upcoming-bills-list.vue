<script lang="ts" setup>
import type {ForecastOccurrence} from "~/models/reports/forecast";
import type {Currency} from "~/models/budget-account/currency";
import {parseISO} from 'date-fns';
import BalanceNumberFormat from "~/components/_atoms/labels/balance-number-format.vue";

const props = defineProps<{
  occurrences: ForecastOccurrence[];
  currency: Currency;
}>();

const {t} = useI18n();
const localeTag = useLocaleTag();

const monthFmt = computed(() => new Intl.DateTimeFormat(localeTag.value, {month: 'long', year: 'numeric'}));
const dayFmt = computed(() => new Intl.DateTimeFormat(localeTag.value, {day: '2-digit', month: 'short'}));

interface MonthGroup {
  key: string;
  label: string;
  items: ForecastOccurrence[];
}

const groups = computed<MonthGroup[]>(() => {
  const byMonth = new Map<string, MonthGroup>();
  for (const occ of props.occurrences ?? []) {
    const key = occ.date.slice(0, 7); // YYYY-MM
    let group = byMonth.get(key);
    if (!group) {
      group = {key, label: monthFmt.value.format(parseISO(occ.date)), items: []};
      byMonth.set(key, group);
    }
    group.items.push(occ);
  }
  return [...byMonth.values()].sort((a, b) => a.key.localeCompare(b.key));
});

// Income reads as +amount, expense as −amount; direction drives the colour too.
function signed(occ: ForecastOccurrence): number {
  return occ.type === 'INCOME' ? occ.amount : -occ.amount;
}
</script>

<template>
  <div>
    <p class="text-sm font-medium text-highlighted mb-2">{{ t('reports.forecast.upcomingTitle') }}</p>

    <p v-if="groups.length === 0" class="text-sm text-muted py-6 text-center">
      {{ t('reports.forecast.noUpcoming') }}
    </p>

    <div v-else class="space-y-4 max-h-72 overflow-y-auto pr-1">
      <div v-for="group in groups" :key="group.key">
        <p class="text-xs uppercase tracking-wide text-muted mb-1.5">{{ group.label }}</p>
        <ul class="space-y-1">
          <li v-for="(occ, i) in group.items"
              :key="`${group.key}-${i}`"
              class="flex items-center gap-2 text-sm">
            <span class="w-14 shrink-0 text-muted tabular-nums">{{ dayFmt.format(parseISO(occ.date)) }}</span>
            <span v-if="occ.categoryColor"
                  class="w-2 h-2 rounded-full shrink-0"
                  :style="{backgroundColor: occ.categoryColor}"/>
            <span class="truncate flex-1 text-default">{{ occ.description || occ.categoryName || '—' }}</span>
            <span class="tabular-nums shrink-0"
                  :class="occ.type === 'INCOME' ? 'text-success' : 'text-error'">
              <BalanceNumberFormat :balance="signed(occ)" :currency="currency"/>
            </span>
          </li>
        </ul>
      </div>
    </div>
  </div>
</template>
