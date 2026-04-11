<script setup lang="ts">
import CreateAccountModal from "~/components/budget-account/create-account-modal.vue";
import {useBudgetAccountsStore} from "~/stores/budgetAccountsStore";

const accountStore = useBudgetAccountsStore();
const isCreateAccountModalVisible = ref(false);

function onCreateAccount(): void {
  isCreateAccountModalVisible.value = true;
}

function onRefresh(): void {
  accountStore.updateAvailableAccounts();
}
</script>

<template>
  <UButton icon="i-lucide-plus" class="w-full sm:w-auto justify-center" @click="onCreateAccount()">
    Create Budget Account
  </UButton>

  <CreateAccountModal v-model="isCreateAccountModalVisible"
                      @created="onRefresh()"/>
</template>
