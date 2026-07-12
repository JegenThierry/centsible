<script lang="ts" setup>
import type {RecurringTransaction} from "~/models/recurring/recurring-transaction";
import {Currency} from "~/models/budget-account/currency";
import {useBudgetAccountsStore} from "~/stores/budgetAccountsStore";
import CategoryBadge from "~/components/_molecules/badges/category-badge.vue";
import FormattedDate from "~/components/_atoms/labels/formatted-date.vue";
import TransactionAmount from "~/components/_molecules/transactions/transaction-amount.vue";
import BalanceNumberFormat from "~/components/_atoms/labels/balance-number-format.vue";
import EditDeleteActions from "~/components/_molecules/buttons/edit-delete-actions.vue";
import AppButton from "~/components/_atoms/ui/app-button.vue";

const props = defineProps<{
  rule: RecurringTransaction;
  currency: Currency;
}>();

defineEmits<{
  (e: 'edit', rule: RecurringTransaction): void;
  (e: 'delete', rule: RecurringTransaction): void;
  (e: 'toggle', rule: RecurringTransaction): void;
}>();

const {t} = useI18n();
const accountsStore = useBudgetAccountsStore();

function accountName(id?: string | null): string {
  if (!id) return '';
  return accountsStore.availableAccounts.find(a => a.id === id)?.name ?? '';
}

const sourceName = computed(() => accountName(props.rule.accountId));
const destinationName = computed(() => accountName(props.rule.destinationAccountId));
const transferCurrency = computed<Currency>(() =>
  accountsStore.availableAccounts.find(a => a.id === props.rule.accountId)?.currency
  ?? props.rule.originalCurrency
  ?? props.currency,
);
</script>

<template>
  <UCard :ui="{body: 'p-4 sm:p-5'}" variant="outline">
    <div class="flex flex-col sm:flex-row sm:items-center gap-4">
      <div class="flex-1 min-w-0">
        <div class="flex items-center gap-2 mb-2">
          <UBadge v-if="rule.isTransfer" color="info" icon="i-lucide-arrow-left-right" variant="subtle">
            {{ t('transactions.transfer.badge') }}
          </UBadge>
          <CategoryBadge v-else
                         :color="rule.category.color"
                         :icon="rule.category.icon"
                         :name="rule.category.name"/>
          <UBadge v-if="!rule.active" color="neutral" variant="subtle">{{ t('transactions.recurring.paused') }}</UBadge>
        </div>
        <p class="font-medium truncate">{{ rule.description }}</p>
        <p v-if="rule.isTransfer && (sourceName || destinationName)" class="text-sm text-neutral-500 mt-1 truncate">
          {{ sourceName }} <UIcon class="inline w-3 h-3" name="i-lucide-arrow-right"/> {{ destinationName }}
        </p>
        <p class="text-sm text-neutral-500 mt-1">
          {{ t(`transactions.recurring.frequency.${rule.frequency}`) }}
          · {{ t('transactions.recurring.nextOn') }}
          <FormattedDate :date="rule.nextRunAt" format="full"/>
          <template v-if="rule.endDate">
            · {{ t('transactions.recurring.ends') }}
            <FormattedDate :date="rule.endDate" format="full"/>
          </template>
        </p>
      </div>

      <div class="flex items-center justify-between sm:justify-end gap-3">
        <TransactionAmount v-if="!rule.isTransfer"
                           :amount="rule.amount"
                           :currency="rule.originalCurrency ?? currency"
                           :type="rule.category.type"/>
        <span v-else class="font-semibold tabular-nums">
          <BalanceNumberFormat :balance="rule.amount" :currency="transferCurrency"/>
        </span>
        <div class="flex items-center gap-1">
          <AppButton :aria-label="rule.active ? t('transactions.recurring.ariaPause') : t('transactions.recurring.ariaResume')"
                   :icon="rule.active ? 'i-lucide-pause' : 'i-lucide-play'"
                   color="neutral"
                   variant="ghost"
                   @click="$emit('toggle', rule)"/>
          <EditDeleteActions :delete-aria-label="t('transactions.recurring.ariaDelete')"
                             :edit-aria-label="t('transactions.recurring.ariaEdit')"
                             @edit="$emit('edit', rule)"
                             @delete="$emit('delete', rule)"/>
        </div>
      </div>
    </div>
  </UCard>
</template>
