<script setup lang="ts">
import type { BudgetAccount } from '~/models/budget-account/budget-account'
import CurrencyBadge from "~/components/_molecules/badges/currency-badge.vue";
import BalanceNumberFormat from "~/components/_molecules/labels/balance-number-format.vue";

defineProps<{
  account: BudgetAccount
}>()

defineEmits<{
  (e: 'click'): void
}>()
</script>

<template>
  <UCard
    class="cursor-pointer hover:ring-2 hover:ring-primary-500 transition-all"
    @click="$emit('click')"
  >
    <template #header>
      <div class="flex items-center justify-between">
        <div class="flex items-center gap-3">
          <CurrencyBadge :currency="account.currency" />
          <span class="font-semibold text-base sm:text-lg">{{ account.name }}</span>
        </div>
        <UIcon class="w-5 h-5 text-neutral-400" name="i-lucide-chevron-right" />
      </div>
    </template>

    <div class="space-y-1">
      <p class="text-xs sm:text-sm text-neutral-500 dark:text-neutral-400">Current Balance</p>
      <p class="text-xl sm:text-2xl font-bold">
        <BalanceNumberFormat
          :balance="account.balance"
          :currency="account.currency"
          format="de-De"
        />
      </p>
    </div>
  </UCard>
</template>
