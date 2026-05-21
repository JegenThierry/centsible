<script lang="ts" setup>
import {useBudgetAccountsStore} from "~/stores/budgetAccountsStore";

const CreateAccountModal = defineAsyncComponent(() => import("~/components/_organisms/accounts/modals/create-account-modal.vue"));

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

  <CreateAccountModal v-if="isCreateAccountModalVisible"
                      v-model="isCreateAccountModalVisible"
                      @created="onRefresh()"/>
</template>
