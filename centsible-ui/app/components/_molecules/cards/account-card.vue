<script lang="ts" setup>
import type {BudgetAccount} from '~/models/budget-account/budget-account'
import CurrencyBadge from "~/components/_molecules/badges/currency-badge.vue";
import BalanceNumberFormat from "~/components/_atoms/labels/balance-number-format.vue";
import BalanceChangeBadge from "~/components/_molecules/badges/balance-change-badge.vue";
import EditDeleteActions from "~/components/_molecules/buttons/edit-delete-actions.vue";

defineProps<{
  account: BudgetAccount
  previousBalance?: number
}>()

defineEmits<{
  (e: 'click'): void
  (e: 'edit'): void
  (e: 'delete'): void
}>()

const {t} = useI18n();
</script>

<template>
  <UCard
    class="cursor-pointer hover:ring-2 hover:ring-primary-500 transition-all h-full"
    @click="$emit('click')"
  >
    <template #header>
      <div class="flex items-center justify-between gap-2">
        <div class="flex items-center gap-3 min-w-0 flex-1">
          <CurrencyBadge class="shrink-0" :currency="account.currency"/>
          <span class="font-semibold text-base sm:text-lg truncate min-w-0" :title="account.name">{{ account.name }}</span>
          <UBadge v-if="account.type" class="shrink-0 whitespace-nowrap" color="neutral" variant="subtle">
            {{ t(`accounts.types.${account.type}`) }}
          </UBadge>
        </div>
        <div class="flex items-center gap-1 shrink-0">
          <EditDeleteActions
            :edit-aria-label="t('accounts.actions.editAria', {name: account.name})"
            :delete-aria-label="t('accounts.actions.deleteAria', {name: account.name})"
            @edit="$emit('edit')"
            @delete="$emit('delete')"
          />
          <UIcon class="w-5 h-5 text-neutral-400" name="i-lucide-chevron-right"/>
        </div>
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
          v-if="previousBalance !== undefined"
          :currency="account.currency"
          :current-balance="account.balance"
          :previous-balance="previousBalance"
        />
      </div>
      <p class="text-xs text-muted">{{ account.currency }} · {{ t('accounts.card.last30Days') }}</p>
    </div>
  </UCard>
</template>
