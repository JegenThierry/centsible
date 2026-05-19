<script lang="ts" setup>
import AccountBalance from "~/components/_organisms/dashboard/account-balance.vue";
import AccountHistoryGraph from "~/components/_organisms/dashboard/account-history-graph.vue";
import AccountHistoryList from "~/components/_organisms/dashboard/account-history-list.vue";
import SpendingByCategoryChart from "~/components/_organisms/dashboard/spending-by-category-chart.vue";
import IncomeVsExpenseChart from "~/components/_organisms/dashboard/income-vs-expense-chart.vue";
import DashboardStats from "~/components/_organisms/dashboard/dashboard-stats.vue";
import RecentTransactions from "~/components/_organisms/dashboard/recent-transactions.vue";
import BudgetsOverview from "~/components/_organisms/dashboard/budgets-overview.vue";
import LoansGlance from "~/components/_organisms/dashboard/loans-glance.vue";
import CategoryDrillSlideover from "~/components/_organisms/dashboard/category-drill-slideover.vue";
import CreateFab from "~/components/_molecules/buttons/create-fab.vue";
import CreateTransactionModal from "~/components/_organisms/transactions/modals/create-transaction-modal.vue";
import CardSkeleton from "~/components/_molecules/skeletons/card-skeleton.vue";
import ChartCardSkeleton from "~/components/_molecules/skeletons/chart-card-skeleton.vue";
import ListCardSkeleton from "~/components/_molecules/skeletons/list-card-skeleton.vue";
import PageHeader from "~/components/_molecules/page/page-header.vue";
import PeriodSelector from "~/components/_molecules/dashboard/period-selector.vue";
import {useBudgetAccountsStore} from "~/stores/budgetAccountsStore";
import {useAccountHistoryStore} from "~/stores/accountHistoryStore";
import {useTransactionStore} from "~/stores/transactionStore";
import type {CategoryDrillPayload} from "~/models/transactions/transaction-filters";

const route = useRoute();
const accountStore = useBudgetAccountsStore();
const historyStore = useAccountHistoryStore();
const transactionStore = useTransactionStore();
const {t} = useI18n();

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
  () => !isAccountReady.value || accountStore.pending || historyStore.pending || transactionStore.pending,
);
const headerDescription = computed(() => {
  if (!isAccountReady.value || !accountStore.activeAccount) return t('accounts.dashboard.headerLoading');
  return t('accounts.dashboard.headerOverview', {name: accountStore.activeAccount.name});
});

async function fetchData() {
  if (!accountStore.activeAccount) return;

  try {
    await Promise.all([
      historyStore.fetchSnapshots(accountStore.activeAccount.id),
      transactionStore.fetchTransactions(accountStore.activeAccount.id)
    ]);
  } catch (error) {
    console.error("Failed to fetch dashboard data", error);
  }
}

function onOpenCreateTransactionModal(): void {
  isCreateTransactionModalVisible.value = true;
}

async function onCreated() {
  await accountStore.updateActiveAccount();
  await fetchData();
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

      <div class="grid grid-cols-1 lg:grid-cols-2 gap-4 sm:gap-6">
        <ListCardSkeleton/>
        <ChartCardSkeleton/>
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
                               :snapshots="historyStore.snapshots"/>
        </div>
      </div>

      <div class="grid grid-cols-1 lg:grid-cols-2 gap-4 sm:gap-6">
        <RecentTransactions :currency="accountStore.activeAccount.currency"
                            :transactions="transactionStore.transactions"/>

        <SpendingByCategoryChart :account-id="accountStore.activeAccount.id"
                                 :currency="accountStore.activeAccount.currency"
                                 @slice-click="onCategorySlice"/>
      </div>

      <IncomeVsExpenseChart :account-id="accountStore.activeAccount.id"
                            :currency="accountStore.activeAccount.currency"/>

      <div class="grid grid-cols-1 lg:grid-cols-2 gap-4 sm:gap-6">
        <BudgetsOverview :currency="accountStore.activeAccount.currency"/>
        <LoansGlance/>
      </div>

      <AccountHistoryList :currency="accountStore.activeAccount.currency"
                          :snapshots="historyStore.snapshots"/>
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
