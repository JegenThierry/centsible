<script lang="ts" setup>
import PageHeader from "~/components/_molecules/page/page-header.vue";
import NetWorthChart from "~/components/_organisms/reports/net-worth-chart.vue";
import NetWorthBreakdownSlideover from "~/components/_organisms/reports/net-worth-breakdown-slideover.vue";
import CategorySpendingChart from "~/components/_organisms/reports/category-spending-chart.vue";
import CashFlowChart from "~/components/_organisms/reports/cash-flow-chart.vue";
import YearOverYearCard from "~/components/_organisms/reports/year-over-year-card.vue";
import BudgetVsActualCard from "~/components/_organisms/reports/budget-vs-actual-card.vue";
import ChartCardSkeleton from "~/components/_molecules/skeletons/chart-card-skeleton.vue";
import {useReportsStore} from "~/stores/reportsStore";
import {useBudgetAccountsStore} from "~/stores/budgetAccountsStore";
import {Currency} from "~/models/budget-account/currency";

const reportsStore = useReportsStore();
const accountsStore = useBudgetAccountsStore();
const {t} = useI18n();

const ranges = [
  {key: '3m', months: 3},
  {key: '6m', months: 6},
  {key: '12m', months: 12},
  {key: '24m', months: 24},
] as const;

const selectedRange = ref<typeof ranges[number]['key']>('6m');

const displayCurrency = computed<Currency>(
  () => accountsStore.availableAccounts[0]?.currency ?? Currency.EUR
);

const rangeItems = computed(() =>
  ranges.map(r => ({label: t(`reports.ranges.${r.key}`), value: r.key}))
);

const breakdownOpen = ref(false);
const breakdownDate = ref<string | null>(null);

function onNetWorthPointClick(date: string) {
  breakdownDate.value = date;
  breakdownOpen.value = true;
}

async function refresh() {
  const range = ranges.find(r => r.key === selectedRange.value) ?? ranges[1]!;
  await Promise.all([
    reportsStore.fetchNetWorth(range.months),
    reportsStore.fetchCategorySpending(range.months),
    reportsStore.fetchCashFlow(range.months),
    reportsStore.fetchYearOverYear(),
    reportsStore.fetchBudgetVsActual(6),
  ]);
}

watch(selectedRange, () => refresh());

onMounted(async () => {
  const loadAccounts = accountsStore.availableAccounts.length === 0
    ? accountsStore.updateAvailableAccounts()
    : Promise.resolve();
  await Promise.all([loadAccounts, refresh()]);
});
</script>

<template>
  <UContainer class="py-6 sm:py-10 space-y-4 sm:space-y-6">
    <PageHeader
      :description="t('reports.page.description')"
      :title="t('reports.page.title')"
    >
      <template #actions>
        <USelect v-model="selectedRange" :items="rangeItems" class="w-40" value-key="value"/>
      </template>
    </PageHeader>

    <ChartCardSkeleton v-if="reportsStore.pending"/>
    <NetWorthChart v-else
                   :currency="displayCurrency"
                   :points="reportsStore.netWorth"
                   @point-click="onNetWorthPointClick"/>

    <div class="grid grid-cols-1 lg:grid-cols-2 gap-4 sm:gap-6">
      <CashFlowChart :currency="displayCurrency" :points="reportsStore.cashFlow"/>
      <CategorySpendingChart :currency="displayCurrency" :series="reportsStore.categorySpending"/>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-2 gap-4 sm:gap-6">
      <YearOverYearCard :currency="displayCurrency" :data="reportsStore.yearOverYear"/>
      <BudgetVsActualCard :currency="displayCurrency" :periods="reportsStore.budgetVsActual"/>
    </div>

    <NetWorthBreakdownSlideover v-model:open="breakdownOpen" :date="breakdownDate"/>
  </UContainer>
</template>
