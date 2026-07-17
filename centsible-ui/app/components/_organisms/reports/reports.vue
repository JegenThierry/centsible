<script lang="ts" setup>
import PageHeader from "~/components/_molecules/page/page-header.vue";
import ReportsKpiStrip from "~/components/_molecules/reports/reports-kpi-strip.vue";
import NetWorthChart from "~/components/_organisms/reports/net-worth-chart.vue";
import NetWorthForecast from "~/components/_organisms/reports/net-worth-forecast.vue";
import NetWorthBreakdownSlideover from "~/components/_organisms/reports/net-worth-breakdown-slideover.vue";
import CategorySpendingChart from "~/components/_organisms/reports/category-spending-chart.vue";
import CashFlowChart from "~/components/_organisms/reports/cash-flow-chart.vue";
import YearOverYearCard from "~/components/_organisms/reports/year-over-year-card.vue";
import BudgetVsActualCard from "~/components/_organisms/reports/budget-vs-actual-card.vue";
import ChartCardSkeleton from "~/components/_molecules/skeletons/chart-card-skeleton.vue";
import DateRangePicker from "~/components/_molecules/reports/date-range-picker.vue";
import AppEmptyState from "~/components/_molecules/feedback/app-empty-state.vue";
import AppButton from "~/components/_atoms/ui/app-button.vue";
import {useReportsStore} from "~/stores/reportsStore";
import {useBudgetAccountsStore} from "~/stores/budgetAccountsStore";
import {Currency} from "~/models/budget-account/currency";
import {watchDebounced} from "@vueuse/core";
import {useReportDateRange} from "~/composables/use-report-date-range";
import {previousIsoDateRange} from "~/utils/date";

const reportsStore = useReportsStore();
const accountsStore = useBudgetAccountsStore();
const {t} = useI18n();

const {preset, customFrom, customTo, resolved, isCustom, isCustomValid} = useReportDateRange('reports');

const displayCurrency = computed<Currency>(
  () => accountsStore.availableAccounts[0]?.currency ?? Currency.EUR
);

const breakdownOpen = ref(false);
const breakdownDate = ref<string | null>(null);

function onNetWorthPointClick(date: string) {
  breakdownDate.value = date;
  breakdownOpen.value = true;
}

async function refresh() {
  if (isCustom.value && !isCustomValid.value) return;
  const range = isCustom.value
    ? {startDate: resolved.value.startDate, endDate: resolved.value.endDate}
    : resolved.value.months;
  // The equal-length window right before the selected range powers the KPI strip's period deltas.
  const previousRange = previousIsoDateRange(resolved.value.startDate, resolved.value.endDate);
  await reportsStore.fetchRangeReports(range, previousRange);
}

// Neither takes the selected range, so neither belongs in refresh().
function loadRangeIndependent(): Promise<unknown> {
  return Promise.all([
    reportsStore.fetchYearOverYear(),
    reportsStore.fetchBudgetVsActual(6),
  ]);
}

// Debounced so a burst of range edits (typing into the custom-date fields) costs one batch instead
// of one per keystroke. It does NOT make ordering safe — two changes further apart than the window
// still dispatch overlapping batches, and the slower one can land last. reportsStore's per-batch
// generation is what discards a superseded batch's results.
watchDebounced([preset, customFrom, customTo], () => refresh(), {debounce: 400});

onMounted(async () => {
  const loadAccounts = accountsStore.availableAccounts.length === 0
    ? accountsStore.updateAvailableAccounts()
    : Promise.resolve();
  await Promise.all([loadAccounts, refresh(), loadRangeIndependent()]);
});
</script>

<template>
  <UContainer class="py-6 sm:py-10 space-y-4 sm:space-y-6">
    <PageHeader
      :description="t('reports.page.description')"
      :title="t('reports.page.title')"
    >
      <template #actions>
        <DateRangePicker v-model:preset="preset"
                         v-model:custom-from="customFrom"
                         v-model:custom-to="customTo"/>
      </template>
    </PageHeader>

    <template v-if="reportsStore.pending">
      <ChartCardSkeleton/>
      <div class="grid grid-cols-1 lg:grid-cols-2 gap-4 sm:gap-6">
        <ChartCardSkeleton/>
        <ChartCardSkeleton/>
      </div>
      <div class="grid grid-cols-1 lg:grid-cols-2 gap-4 sm:gap-6">
        <ChartCardSkeleton/>
        <ChartCardSkeleton/>
      </div>
    </template>

    <AppEmptyState v-else-if="reportsStore.error"
                   icon="i-lucide-triangle-alert"
                   :title="t('common.states.error')">
      <template #actions>
        <AppButton class="w-full sm:w-auto justify-center"
                   color="neutral"
                   variant="soft"
                   icon="i-lucide-refresh-cw"
                   @click="refresh">
          {{ t('common.actions.retry') }}
        </AppButton>
      </template>
    </AppEmptyState>

    <template v-else>
      <ReportsKpiStrip :currency="displayCurrency"
                       :current="reportsStore.cashFlow"
                       :previous="reportsStore.cashFlowPrevious"/>

      <NetWorthChart :currency="displayCurrency"
                     :points="reportsStore.netWorth"
                     @point-click="onNetWorthPointClick"/>

      <NetWorthForecast :currency="displayCurrency"/>

      <div class="grid grid-cols-1 lg:grid-cols-2 gap-4 sm:gap-6">
        <CashFlowChart :currency="displayCurrency" :points="reportsStore.cashFlow"/>
        <CategorySpendingChart :currency="displayCurrency" :series="reportsStore.categorySpending"/>
      </div>

      <div class="grid grid-cols-1 lg:grid-cols-2 gap-4 sm:gap-6">
        <YearOverYearCard :currency="displayCurrency" :data="reportsStore.yearOverYear"/>
        <BudgetVsActualCard :currency="displayCurrency" :periods="reportsStore.budgetVsActual"/>
      </div>
    </template>

    <NetWorthBreakdownSlideover v-model:open="breakdownOpen" :date="breakdownDate"/>
  </UContainer>
</template>
