<script lang="ts" setup>
import CreateAccountModal from "~/components/_organisms/accounts/modals/create-account-modal.vue";
import {useBudgetAccountsStore} from "~/stores/budgetAccountsStore";

const accountStore = useBudgetAccountsStore();
const {t} = useI18n();
const isCreateAccountModalVisible = ref(false);

function onCreateAccount(): void {
  isCreateAccountModalVisible.value = true;
}

function onRefresh(): void {
  accountStore.updateAvailableAccounts();
}
</script>

<template>
  <UButton class="w-full sm:w-auto justify-center" icon="i-lucide-plus" @click="onCreateAccount()">
    {{ t('accounts.buttons.createAccount') }}
  </UButton>

  <CreateAccountModal v-model="isCreateAccountModalVisible"
                      @created="onRefresh()"/>
</template>
