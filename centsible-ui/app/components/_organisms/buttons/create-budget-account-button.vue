<script lang="ts" setup>
import AppButton from "~/components/_atoms/ui/app-button.vue";
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
  <AppButton class="w-full sm:w-auto justify-center" icon="i-lucide-plus" @click="onCreateAccount()">
    {{ t('accounts.buttons.createAccount') }}
  </AppButton>

  <CreateAccountModal v-if="isCreateAccountModalVisible"
                      v-model="isCreateAccountModalVisible"
                      @created="onRefresh()"/>
</template>
