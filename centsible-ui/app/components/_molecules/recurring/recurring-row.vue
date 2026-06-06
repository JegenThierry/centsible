<script lang="ts" setup>
import type {RecurringTransaction} from "~/models/recurring/recurring-transaction";
import {Currency} from "~/models/budget-account/currency";
import CategoryBadge from "~/components/_molecules/badges/category-badge.vue";
import FormattedDate from "~/components/_atoms/labels/formatted-date.vue";
import TransactionAmount from "~/components/_molecules/transactions/transaction-amount.vue";
import EditDeleteActions from "~/components/_molecules/buttons/edit-delete-actions.vue";
import AppButton from "~/components/_atoms/ui/app-button.vue";

defineProps<{
  rule: RecurringTransaction;
  currency: Currency;
}>();

defineEmits<{
  (e: 'edit', rule: RecurringTransaction): void;
  (e: 'delete', rule: RecurringTransaction): void;
  (e: 'toggle', rule: RecurringTransaction): void;
}>();

const {t} = useI18n();
</script>

<template>
  <UCard :ui="{body: 'p-4 sm:p-5'}" variant="outline">
    <div class="flex flex-col sm:flex-row sm:items-center gap-4">
      <div class="flex-1 min-w-0">
        <div class="flex items-center gap-2 mb-2">
          <CategoryBadge :color="rule.category.color"
                         :icon="rule.category.icon"
                         :name="rule.category.name"/>
          <UBadge v-if="!rule.active" color="neutral" variant="subtle">{{ t('transactions.recurring.paused') }}</UBadge>
        </div>
        <p class="font-medium truncate">{{ rule.description }}</p>
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
        <TransactionAmount :amount="rule.amount"
                           :currency="rule.originalCurrency ?? currency"
                           :type="rule.category.type"/>
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
