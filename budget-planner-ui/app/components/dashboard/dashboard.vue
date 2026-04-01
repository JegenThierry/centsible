<script setup lang="ts">
import AccountBalance from "~/components/dashboard/cards/account-balance.vue";
import CreateFab from "~/components/_molecules/buttons/create-fab.vue";
import CreateTransactionModal from "~/components/transactions/modals/create-transaction-modal.vue";

const accountStore = useBudgetAccountsStore();

const isCreateTransactionModalVisible = ref(false);

function onOpenCreateTransactionModal(): void {
  isCreateTransactionModalVisible.value = true;
}

onMounted(() => {
  if (accountStore.availableAccounts.length === 0) {
    accountStore.updateAvailableAccounts();
  }
});
</script>

<template>
  <UContainer v-if="accountStore.activeAccount != null" class="flex p-4 lg:p-10 gap-4 lg:gap-10 flex-wrap">
    <AccountBalance :balance="accountStore.activeAccount.balance"
                    :account-name="accountStore.activeAccount.name"
                    :currency="accountStore.activeAccount.currency" />
  </UContainer>

  <CreateFab @click="onOpenCreateTransactionModal" />

  <CreateTransactionModal v-if="isCreateTransactionModalVisible"
                          v-model:open="isCreateTransactionModalVisible" />

</template>
