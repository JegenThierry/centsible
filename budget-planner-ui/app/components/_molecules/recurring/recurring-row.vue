<script lang="ts" setup>
import type {RecurringTransaction} from "~/models/recurring/recurring-transaction";
import {FREQUENCY_LABELS} from "~/models/recurring/recurring-transaction";
import {Currency} from "~/models/budget-account/currency";
import CategoryBadge from "~/components/_molecules/badges/category-badge.vue";
import FormattedDate from "~/components/_atoms/labels/formatted-date.vue";
import TransactionAmount from "~/components/_molecules/transactions/transaction-amount.vue";

defineProps<{
  rule: RecurringTransaction;
  currency: Currency;
}>();

defineEmits<{
  (e: 'edit', rule: RecurringTransaction): void;
  (e: 'delete', rule: RecurringTransaction): void;
  (e: 'toggle', rule: RecurringTransaction): void;
}>();
</script>

<template>
  <UCard :ui="{body: 'p-4 sm:p-5'}" variant="outline">
    <div class="flex flex-col sm:flex-row sm:items-center gap-4">
      <div class="flex-1 min-w-0">
        <div class="flex items-center gap-2 mb-2">
          <CategoryBadge :color="rule.category.color"
                         :icon="rule.category.icon"
                         :name="rule.category.name"/>
          <UBadge v-if="!rule.active" color="neutral" variant="subtle">Paused</UBadge>
        </div>
        <p class="font-medium truncate">{{ rule.description }}</p>
        <p class="text-sm text-neutral-500 mt-1">
          {{ FREQUENCY_LABELS[rule.frequency] }}
          · next on
          <FormattedDate :date="rule.nextRunAt" format="full"/>
          <template v-if="rule.endDate">
            · ends
            <FormattedDate :date="rule.endDate" format="full"/>
          </template>
        </p>
      </div>

      <div class="flex items-center justify-between sm:justify-end gap-3">
        <TransactionAmount :amount="rule.amount"
                           :currency="currency"
                           :type="rule.category.type"/>
        <div class="flex items-center gap-1">
          <UButton :aria-label="rule.active ? 'Pause rule' : 'Resume rule'"
                   :icon="rule.active ? 'i-lucide-pause' : 'i-lucide-play'"
                   color="neutral"
                   variant="ghost"
                   @click="$emit('toggle', rule)"/>
          <UButton aria-label="Edit rule"
                   color="neutral"
                   icon="i-lucide-pencil"
                   variant="ghost"
                   @click="$emit('edit', rule)"/>
          <UButton aria-label="Delete rule"
                   color="error"
                   icon="i-lucide-trash"
                   variant="ghost"
                   @click="$emit('delete', rule)"/>
        </div>
      </div>
    </div>
  </UCard>
</template>
