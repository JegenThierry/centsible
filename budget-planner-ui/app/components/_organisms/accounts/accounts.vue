<script lang="ts" setup>
import NoAccountAction from "~/components/_organisms/accounts/no-account-action.vue";
import {useBudgetAccountsStore} from "~/stores/budgetAccountsStore";
import PageHeader from "~/components/_molecules/page/page-header.vue";
import AccountCard from "~/components/_organisms/cards/account-card.vue";
import CreateBudgetAccountButton from "~/components/_organisms/buttons/create-budget-account-button.vue";
import CardSkeleton from "~/components/_molecules/skeletons/card-skeleton.vue";
import LoadingAnimation from "~/components/_atoms/animations/loading-animation.vue";

const accountStore = useBudgetAccountsStore();

function onRefresh(): void {
  accountStore.updateAvailableAccounts();
}

accountStore.clearActiveAccount();
accountStore.updateAvailableAccounts();
</script>

<template>
  <UContainer class="py-6 sm:py-10">
    <PageHeader
      title="Accounts"
      description="Select an account to manage your budget"
    >
      <template #actions>
        <CreateBudgetAccountButton />
      </template>
    </PageHeader>

    <div v-if="accountStore.pending && accountStore.availableAccounts.length > 0" class="flex justify-center mb-6">
      <LoadingAnimation />
    </div>

    <template v-if="accountStore.availableAccounts.length === 0">
      <div v-if="accountStore.pending" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 sm:gap-6">
        <CardSkeleton v-for="i in 3" :key="i" />
      </div>

      <div v-else class="flex justify-center py-10 sm:py-20">
        <NoAccountAction @refresh-accounts="onRefresh"/>
      </div>
    </template>

    <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 sm:gap-6">
      <AccountCard
        v-for="account in accountStore.availableAccounts"
        :key="account.id"
        :account="account"
        @click="navigateTo(`/${account.id}/dashboard`)"
      />
    </div>
  </UContainer>
</template>
