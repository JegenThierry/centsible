<script lang="ts" setup>
import NoAccountAction from "~/components/_organisms/accounts/no-account-action.vue";
import {useBudgetAccountsStore} from "~/stores/budgetAccountsStore";
import PageHeader from "~/components/_molecules/page/page-header.vue";
import AccountCard from "~/components/_molecules/cards/account-card.vue";
import AccountsOverview from "~/components/_molecules/accounts/accounts-overview.vue";
import AccountsBalanceBreakdown from "~/components/_molecules/accounts/accounts-balance-breakdown.vue";
import CreateBudgetAccountButton from "~/components/_organisms/buttons/create-budget-account-button.vue";
import CardSkeleton from "~/components/_molecules/skeletons/card-skeleton.vue";
import LoadingAnimation from "~/components/_atoms/animations/loading-animation.vue";
import ExportButton from "~/components/_molecules/exports/export-button.vue";
import EditAccountModal from "~/components/_organisms/accounts/modals/edit-account-modal.vue";
import DeleteAccountModal from "~/components/_organisms/accounts/modals/delete-account-modal.vue";
import type {BudgetAccount} from "~/models/budget-account/budget-account";
import {todayIsoDate} from "~/utils/date";

const accountStore = useBudgetAccountsStore();
const {previousBalances, refresh: refreshTrends} = useAccountBalanceTrends(30);
const {t} = useI18n();

const editTarget = ref<BudgetAccount>();
const isEditOpen = ref(false);
const deleteTarget = ref<BudgetAccount>();
const isDeleteOpen = ref(false);

function onEdit(account: BudgetAccount): void {
  editTarget.value = account;
  isEditOpen.value = true;
}

function onDelete(account: BudgetAccount): void {
  deleteTarget.value = account;
  isDeleteOpen.value = true;
}

function load(): void {
  accountStore.updateAvailableAccounts();
  refreshTrends();
}

function onRefresh(): void {
  load();
}

accountStore.clearActiveAccount();

// `onboarding-guard` has already fetched the list; mutations patch the store in place, so it stays current.
onMounted(() => {
  if (accountStore.availableAccounts.length === 0) {
    accountStore.updateAvailableAccounts();
  }
  refreshTrends();
});
</script>

<template>
  <UContainer class="py-6 sm:py-10">
    <PageHeader
      :description="t('accounts.list.description')"
      :title="t('accounts.list.title')"
    >
      <template #actions>
        <div class="flex gap-2">
          <ExportButton
            v-if="accountStore.availableAccounts.length > 0"
            :default-title="t('accounts.list.exportSummaryTitle', {date: todayIsoDate()})"
            :params-builder="() => ({ kind: 'ACCOUNTS_SUMMARY' })"
            :label="t('accounts.list.exportSummaryLabel')"
            type="ACCOUNTS_SUMMARY"
          />
          <CreateBudgetAccountButton/>
        </div>
      </template>
    </PageHeader>

    <div v-if="accountStore.pending && accountStore.availableAccounts.length > 0" class="flex justify-center mb-6">
      <LoadingAnimation/>
    </div>

    <template v-if="accountStore.availableAccounts.length === 0">
      <div v-if="accountStore.pending" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 sm:gap-6">
        <CardSkeleton v-for="i in 3" :key="i"/>
      </div>

      <div v-else class="flex justify-center py-10 sm:py-20">
        <NoAccountAction @refresh-accounts="onRefresh"/>
      </div>
    </template>

    <template v-else>
      <AccountsOverview
        :accounts="accountStore.availableAccounts"
        :previous-balances="previousBalances"
        class="mb-4 sm:mb-6"
      />

      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 sm:gap-6">
        <AccountCard
          v-for="account in accountStore.availableAccounts"
          :key="account.id"
          :account="account"
          :previous-balance="previousBalances[account.id]"
          @click="navigateTo(`/${account.id}/dashboard`)"
          @edit="onEdit(account)"
          @delete="onDelete(account)"
        />
      </div>

      <AccountsBalanceBreakdown
        v-if="accountStore.availableAccounts.length > 1"
        :accounts="accountStore.availableAccounts"
        class="mt-4 sm:mt-6"
      />
    </template>

    <EditAccountModal v-if="editTarget" v-model="isEditOpen" :account="editTarget"/>
    <DeleteAccountModal v-model:open="isDeleteOpen" :account="deleteTarget" @deleted="refreshTrends"/>
  </UContainer>
</template>
