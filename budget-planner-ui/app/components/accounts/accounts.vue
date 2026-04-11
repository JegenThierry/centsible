<script lang="ts" setup>
import NoAccountAction from "~/components/_organisms/no-account-action.vue";
import {useBudgetAccountsStore} from "~/stores/budgetAccountsStore";
import CurrencyBadge from "~/components/_molecules/badges/currency-badge.vue";
import BalanceNumberFormat from "~/components/_molecules/labels/balance-number-format.vue";
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
    <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-6 sm:mb-8">
      <div>
        <h1 class="text-2xl sm:text-3xl font-bold tracking-tight">Accounts</h1>
        <p class="text-sm sm:text-base text-neutral-500 dark:text-neutral-400">Select an account to manage your budget</p>
      </div>
      <CreateBudgetAccountButton />
    </div>

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
      <UCard v-for="account in accountStore.availableAccounts"
             :key="account.id"
             class="cursor-pointer hover:ring-2 hover:ring-primary-500 transition-all"
             @click="navigateTo(`/${account.id}/dashboard`)">
        <template #header>
          <div class="flex items-center justify-between">
            <div class="flex items-center gap-3">
              <CurrencyBadge :currency="account.currency"/>
              <span class="font-semibold text-base sm:text-lg">{{ account.name }}</span>
            </div>
            <UIcon class="w-5 h-5 text-neutral-400" name="i-lucide-chevron-right"/>
          </div>
        </template>

        <div class="space-y-1">
          <p class="text-xs sm:text-sm text-neutral-500 dark:text-neutral-400">Current Balance</p>
          <p class="text-xl sm:text-2xl font-bold">
            <BalanceNumberFormat :balance="account.balance"
                                 :currency="account.currency"
                                 format="de-De"/>
          </p>
        </div>
      </UCard>
    </div>
  </UContainer>
</template>
