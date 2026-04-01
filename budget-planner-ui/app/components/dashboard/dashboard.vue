<script setup lang="ts">
import NoAccountAction from "~/components/dashboard/no-account-action.vue";
import CreateAccountModal from "~/components/budget-account/create-account-modal.vue";
import AccountBalance from "~/components/dashboard/cards/account-balance.vue";
import CreateFab from "~/components/_molecules/buttons/create-fab.vue";
import CreateTransactionModal from "~/components/transactions/modals/create-transaction-modal.vue";

const accountStore = useBudgetAccountsStore();

const isCreateAccountModalVisible = ref(false);
const isCreateTransactionModalVisible = ref(false);

function onCreateAccount(): void {
  isCreateAccountModalVisible.value = true;
}

function onOpenCreateTransactionModal(): void {
  isCreateTransactionModalVisible.value = true;
}

function refreshAccountsAndSelectDefault(){
  accountStore.updateAvailableAccounts().then(() => {
    console.log("Available accounts:", accountStore.availableAccounts);
    if (accountStore.availableAccounts.length === 0) {
      return;
    }

    accountStore.activeAccount = accountStore.availableAccounts[0];
  })
}

function onRefresh(): void {
  refreshAccountsAndSelectDefault();
}

onMounted(() => refreshAccountsAndSelectDefault());
</script>

<template>
  <UContainer v-if="accountStore.availableAccounts.length === 0" class="flex justify-center p-4 lg:p-10">
    <NoAccountAction @create-budget-account="onCreateAccount"
                     @refresh-accounts="onRefresh"/>
  </UContainer>

  <UContainer v-if="accountStore.activeAccount != null" class="flex p-4 lg:p-10 gap-4 lg:gap-10 flex-wrap">
    <AccountBalance :balance="accountStore.activeAccount.balance"
                    :account-name="accountStore.activeAccount.name"
                    :currency="accountStore.activeAccount.currency" />
  </UContainer>

  <CreateAccountModal v-model="isCreateAccountModalVisible"
                      @created="refreshAccountsAndSelectDefault"/>

  <CreateFab @click="onOpenCreateTransactionModal" />

  <CreateTransactionModal v-if="isCreateTransactionModalVisible"
                          v-model:open="isCreateTransactionModalVisible" />

</template>
