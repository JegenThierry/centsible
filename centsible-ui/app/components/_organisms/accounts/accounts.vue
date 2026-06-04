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
import {todayIsoDate} from "~/utils/date";

const accountStore = useBudgetAccountsStore();
const {t} = useI18n();

function onRefresh(): void {
  accountStore.updateAvailableAccounts();
}

accountStore.clearActiveAccount();
accountStore.updateAvailableAccounts();
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
      <AccountsOverview :accounts="accountStore.availableAccounts" class="mb-4 sm:mb-6"/>

      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 sm:gap-6">
        <AccountCard
          v-for="account in accountStore.availableAccounts"
          :key="account.id"
          :account="account"
          @click="navigateTo(`/${account.id}/dashboard`)"
        />
      </div>

      <AccountsBalanceBreakdown
        v-if="accountStore.availableAccounts.length > 1"
        :accounts="accountStore.availableAccounts"
        class="mt-4 sm:mt-6"
      />
    </template>
  </UContainer>
</template>
