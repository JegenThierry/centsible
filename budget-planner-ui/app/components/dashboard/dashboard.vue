<script setup lang="ts">
import AccountBalance from "~/components/dashboard/cards/account-balance.vue";
import AccountHistoryGraph from "~/components/dashboard/cards/account-history-graph.vue";
import AccountHistoryList from "~/components/dashboard/cards/account-history-list.vue";
import TransactionsByCategory from "~/components/dashboard/cards/transactions-by-category.vue";
import CreateFab from "~/components/_molecules/buttons/create-fab.vue";
import CreateTransactionModal from "~/components/transactions/modals/create-transaction-modal.vue";
import CardSkeleton from "~/components/_molecules/skeletons/card-skeleton.vue";
import ChartCardSkeleton from "~/components/_molecules/skeletons/chart-card-skeleton.vue";
import ListCardSkeleton from "~/components/_molecules/skeletons/list-card-skeleton.vue";
import {useBudgetAccountsStore} from "~/stores/budgetAccountsStore";
import {useAccountHistoryStore} from "~/stores/accountHistoryStore";
import {useTransactionStore} from "~/stores/transactionStore";

const accountStore = useBudgetAccountsStore();
const historyStore = useAccountHistoryStore();
const transactionStore = useTransactionStore();

const isCreateTransactionModalVisible = ref(false);

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
  if (newId) fetchData();
}, {immediate: true});

onMounted(() => {
  if (accountStore.activeAccount) {
    fetchData();
  }
});
</script>

<template>
  <div v-if="accountStore.pending || (accountStore.activeAccount && (historyStore.pending || transactionStore.pending))" class="p-4 sm:p-6 lg:p-10 space-y-4 sm:space-y-6">
    <div class="grid grid-cols-1 md:grid-cols-3 gap-4 sm:gap-6">
      <CardSkeleton />
      <div class="md:col-span-2">
        <ChartCardSkeleton />
      </div>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-2 gap-4 sm:gap-6">
      <ChartCardSkeleton />
      <ListCardSkeleton />
    </div>
  </div>

  <div v-else-if="accountStore.activeAccount" class="p-4 sm:p-6 lg:p-10 space-y-4 sm:space-y-6">
    <div class="grid grid-cols-1 md:grid-cols-3 gap-4 sm:gap-6">
      <AccountBalance :balance="accountStore.activeAccount.balance"
                      :initial-balance="accountStore.activeAccount.initialBalance"
                      :account-name="accountStore.activeAccount.name"
                      :currency="accountStore.activeAccount.currency" />

      <div class="md:col-span-2">
         <AccountHistoryGraph :snapshots="historyStore.snapshots"
                             :currency="accountStore.activeAccount.currency" />
      </div>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-2 gap-4 sm:gap-6">
      <TransactionsByCategory :transactions="transactionStore.transactions"
                              :currency="accountStore.activeAccount.currency" />

      <AccountHistoryList :snapshots="historyStore.snapshots"
                          :currency="accountStore.activeAccount.currency" />
    </div>
  </div>

  <CreateFab @click="onOpenCreateTransactionModal" />

  <CreateTransactionModal v-if="isCreateTransactionModalVisible"
                          v-model:open="isCreateTransactionModalVisible"
                          @created="onCreated()" />
</template>
