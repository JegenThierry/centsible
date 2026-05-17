<script lang="ts" setup>
import type {Currency} from "~/models/budget-account/currency";
import BalanceNumberFormat from "~/components/_atoms/labels/balance-number-format.vue";
import FormattedDate from "~/components/_atoms/labels/formatted-date.vue";
import type {BudgetAccountSnapshot} from "~/models/budget-account/budget-account";

defineProps<{
  snapshots: BudgetAccountSnapshot[],
  currency: Currency
}>();

const {t} = useI18n();
</script>

<template>
  <UCard>
    <template #header>
      <div class="flex items-center justify-between">
        <h3 class="text-base font-semibold text-highlighted">
          {{ t('accounts.dashboard.accountHistory') }}
        </h3>
      </div>
    </template>

    <div class="overflow-y-auto max-h-64">
      <ul class="divide-y divide-neutral-200 dark:divide-neutral-800">
        <li v-for="snapshot in snapshots"
            :key="snapshot.createdAt"
            class="py-2 sm:py-3 flex justify-between items-center">
          <FormattedDate :date="snapshot.createdAt" class="text-sm text-muted" format="date"/>
          <span class="text-sm font-medium text-highlighted">
            <BalanceNumberFormat :balance="snapshot.balance"
                                 :currency="currency"/>
          </span>
        </li>
      </ul>
    </div>
  </UCard>
</template>
