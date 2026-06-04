<script lang="ts" setup>
import type {BudgetAccount} from '~/models/budget-account/budget-account'
import CurrencyBadge from "~/components/_molecules/badges/currency-badge.vue";
import BalanceNumberFormat from "~/components/_atoms/labels/balance-number-format.vue";
import BalanceChangeBadge from "~/components/_molecules/badges/balance-change-badge.vue";

defineProps<{
  account: BudgetAccount
}>()

defineEmits<{
  (e: 'click'): void
}>()

const {t} = useI18n();
</script>

<template>
  <UCard
    class="cursor-pointer hover:ring-2 hover:ring-primary-500 transition-all"
    @click="$emit('click')"
  >
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
      <p class="text-xs sm:text-sm text-muted">{{ t('accounts.card.currentBalance') }}</p>
      <div class="flex items-center gap-2 flex-wrap">
        <p :class="[
          'text-2xl sm:text-3xl font-bold tracking-tight tabular-nums',
          account.balance < 0 ? 'text-error' : 'text-highlighted',
        ]">
          <BalanceNumberFormat
            :balance="account.balance"
            :currency="account.currency"
          />
        </p>
        <BalanceChangeBadge
          :currency="account.currency"
          :current-balance="account.balance"
          :previous-balance="account.initialBalance"
        />
      </div>
      <p class="text-xs text-muted">{{ account.currency }} · {{ t('accounts.card.sinceOpening') }}</p>
    </div>
  </UCard>
</template>
