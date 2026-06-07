<script lang="ts" setup>
import adze from 'adze'
import AccountBalance from "~/components/_molecules/dashboard/account-balance.vue";
import AccountHistoryGraph from "~/components/_molecules/dashboard/account-history-graph.vue";
import AccountHistoryList from "~/components/_molecules/dashboard/account-history-list.vue";
import SpendingByCategoryChart from "~/components/_organisms/dashboard/spending-by-category-chart.vue";
import IncomeVsExpenseChart from "~/components/_organisms/dashboard/income-vs-expense-chart.vue";
import ActivityHeatmap from "~/components/_organisms/dashboard/activity-heatmap.vue";
import DashboardStats from "~/components/_organisms/dashboard/dashboard-stats.vue";
import RecentTransactions from "~/components/_organisms/dashboard/recent-transactions.vue";
import BudgetsOverview from "~/components/_organisms/dashboard/budgets-overview.vue";
import LoansGlance from "~/components/_organisms/dashboard/loans-glance.vue";
import CategoryDrillSlideover from "~/components/_organisms/dashboard/category-drill-slideover.vue";
import CreateFab from "~/components/_molecules/buttons/create-fab.vue";
import CardSkeleton from "~/components/_molecules/skeletons/card-skeleton.vue";
import ChartCardSkeleton from "~/components/_molecules/skeletons/chart-card-skeleton.vue";
import ListCardSkeleton from "~/components/_molecules/skeletons/list-card-skeleton.vue";
import PageHeader from "~/components/_molecules/page/page-header.vue";
import PeriodSelector from "~/components/_molecules/dashboard/period-selector.vue";
import {useBudgetAccountsStore} from "~/stores/budgetAccountsStore";
import {useBudgetsStore} from "~/stores/budgetsStore";
import {useLoansStore} from "~/stores/loansStore";
import {useTransactionService} from "~/services/transactions/transaction-service";
import {useTransactionList} from "~/components/_organisms/transactions/utils/use-transaction-list";
import {invalidateMonthlyAggregates, prefetchMonthlyAggregates} from "~/composables/use-monthly-aggregates";
import {invalidateCategoryAggregates, prefetchCategoryAggregates} from "~/composables/use-category-aggregates";
import {invalidateDailyAggregates, prefetchDailyAggregates} from "~/composables/use-daily-aggregates";
import {invalidateAccountSnapshots, useAccountSnapshots} from "~/composables/use-account-snapshots";
import {useDashboardPeriod} from "~/composables/use-dashboard-period";
import type {CategoryDrillPayload} from "~/models/transactions/transaction-filters";

const CreateTransactionModal = defineAsyncComponent(() => import("~/components/_organisms/transactions/modals/create-transaction-modal.vue"));

const route = useRoute();
const accountStore = useBudgetAccountsStore();
const budgetsStore = useBudgetsStore();
const loansStore = useLoansStore();
const transactionService = useTransactionService(useApi());

// Recent-transactions widget shares the one transaction data path (pagination + error state) instead
// of a stunted dedicated store; we only ever load the first page here.
const {
  transactions: recentTransactions,
  loading: recentLoading,
  error: recentError,
  loadTransactions: loadRecentTransactions,
} = useTransactionList(transactionService, accountStore, 25);
const {window} = useDashboardPeriod();
const {t} = useI18n();

const {data: snapshots, loading: snapshotsLoading, reload: reloadSnapshots} = useAccountSnapshots(
  () => accountStore.activeAccount?.id ?? '',
  () => window.value.fromIso,
  () => window.value.toIso,
);
const recentSnapshots = computed(() => snapshots.value.slice(-30));

const isCreateTransactionModalVisible = ref(false);
const isCategoryDrillOpen = ref(false);
const categoryDrill = ref<CategoryDrillPayload | null>(null);

function onCategorySlice(payload: CategoryDrillPayload) {
  categoryDrill.value = payload;
  isCategoryDrillOpen.value = true;
}

const routeAccountId = computed(() => String(route.params.accountId ?? ''));
const isAccountReady = computed(
  () => !!accountStore.activeAccount && accountStore.activeAccount.id === routeAccountId.value,
);
const isLoading = computed(
  () => !isAccountReady.value || accountStore.pending || recentLoading.value
    || (snapshotsLoading.value && snapshots.value.length === 0),
);
const headerDescription = computed(() => {
  if (!isAccountReady.value || !accountStore.activeAccount) return t('accounts.dashboard.headerLoading');
  return t('accounts.dashboard.headerOverview', {name: accountStore.activeAccount.name});
});

async function fetchData() {
  if (!accountStore.activeAccount) return;
  const id = accountStore.activeAccount.id;
  const periodWindow = window.value;

  const tasks: Promise<unknown>[] = [
    loadRecentTransactions(true),
    prefetchMonthlyAggregates(transactionService, id, periodWindow.months),
    prefetchCategoryAggregates(transactionService, id, periodWindow.fromIso, periodWindow.toIso),
    prefetchDailyAggregates(transactionService, id, 371),
  ];
  if (budgetsStore.items.length === 0) {
    tasks.push(budgetsStore.fetchCurrentMonth());
  }
  if (!loansStore.allLoansLoaded) {
    tasks.push(loansStore.refreshAllLoans().catch(e => adze.ns('dashboard').error('Failed to load loans', e)));
  }
  if (!loansStore.outstandingLoaded) {
    tasks.push(loansStore.refreshOutstanding());
  }

  try {
    await Promise.all(tasks);
  } catch (error) {
    adze.ns('dashboard').error("Failed to fetch dashboard data", error);
  }
}

function onOpenCreateTransactionModal(): void {
  isCreateTransactionModalVisible.value = true;
}

async function onCreated() {
  invalidateMonthlyAggregates();
  invalidateCategoryAggregates();
  invalidateDailyAggregates();
  invalidateAccountSnapshots();
  await accountStore.updateActiveAccount();
  await Promise.all([fetchData(), reloadSnapshots()]);
}

watch(() => accountStore.activeAccount?.id, (newId) => {
  if (newId && newId === routeAccountId.value) fetchData();
}, {immediate: true});
</script>

<template>
  <UContainer class="py-6 sm:py-10 space-y-4 sm:space-y-6">
    <PageHeader
      :description="headerDescription"
      :title="t('accounts.dashboard.title')"
    >
      <template #actions>
        <PeriodSelector v-if="isAccountReady"/>
      </template>
    </PageHeader>

    <div v-if="isLoading" class="space-y-4 sm:space-y-6">
      <div class="grid grid-cols-1 sm:grid-cols-3 gap-4 sm:gap-6">
        <CardSkeleton v-for="i in 3" :key="i"/>
      </div>

      <div class="grid grid-cols-1 md:grid-cols-3 gap-4 sm:gap-6">
        <CardSkeleton/>
        <div class="md:col-span-2">
          <ChartCardSkeleton/>
        </div>
      </div>

      <ChartCardSkeleton/>

      <div class="grid grid-cols-1 lg:grid-cols-2 gap-4 sm:gap-6">
        <ListCardSkeleton/>
        <ChartCardSkeleton/>
      </div>

      <div class="grid grid-cols-1 lg:grid-cols-2 gap-4 sm:gap-6">
        <ChartCardSkeleton/>
        <ListCardSkeleton/>
      </div>

      <ListCardSkeleton/>
    </div>

    <div v-else-if="accountStore.activeAccount" class="space-y-4 sm:space-y-6">
      <DashboardStats :account-id="accountStore.activeAccount.id"
                      :currency="accountStore.activeAccount.currency"/>

      <div class="grid grid-cols-1 md:grid-cols-3 gap-4 sm:gap-6">
        <AccountBalance :account-name="accountStore.activeAccount.name"
                        :balance="accountStore.activeAccount.balance"
                        :currency="accountStore.activeAccount.currency"
                        :initial-balance="accountStore.activeAccount.initialBalance"/>

        <div class="md:col-span-2">
          <AccountHistoryGraph :currency="accountStore.activeAccount.currency"
                               :snapshots="snapshots"/>
        </div>
      </div>

      <ActivityHeatmap :account-id="accountStore.activeAccount.id"
                       :currency="accountStore.activeAccount.currency"/>

      <div class="grid grid-cols-1 lg:grid-cols-2 gap-4 sm:gap-6">
        <RecentTransactions :currency="accountStore.activeAccount.currency"
                            :transactions="recentTransactions"
                            :error="recentError"
                            @retry="loadRecentTransactions(true)"/>

        <SpendingByCategoryChart :account-id="accountStore.activeAccount.id"
                                 :currency="accountStore.activeAccount.currency"
                                 @slice-click="onCategorySlice"/>
      </div>

      <div class="grid grid-cols-1 lg:grid-cols-2 gap-4 sm:gap-6">
        <IncomeVsExpenseChart :account-id="accountStore.activeAccount.id"
                              :currency="accountStore.activeAccount.currency"/>
        <AccountHistoryList :currency="accountStore.activeAccount.currency"
                            :snapshots="recentSnapshots"/>
      </div>

      <LoansGlance/>

      <BudgetsOverview :currency="accountStore.activeAccount.currency"/>
    </div>

    <CreateFab @create="onOpenCreateTransactionModal"/>

    <CreateTransactionModal v-if="isCreateTransactionModalVisible"
                            v-model:open="isCreateTransactionModalVisible"
                            @created="onCreated()"/>

    <CategoryDrillSlideover v-if="accountStore.activeAccount && categoryDrill"
                            v-model:open="isCategoryDrillOpen"
                            :account-id="accountStore.activeAccount.id"
                            :currency="accountStore.activeAccount.currency"
                            :category-id="categoryDrill.categoryId"
                            :category-name="categoryDrill.categoryName"
                            :from-date="categoryDrill.fromDate"
                            :to-date="categoryDrill.toDate"/>
  </UContainer>
</template>
