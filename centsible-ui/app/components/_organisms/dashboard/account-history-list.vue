<script lang="ts" setup>
import type {Currency} from "~/models/budget-account/currency";
import BalanceNumberFormat from "~/components/_molecules/labels/balance-number-format.vue";
import {format, parseISO} from 'date-fns';
import type {BudgetAccountSnapshot} from "~/models/budget-account/budget-account";

defineProps<{
  snapshots: BudgetAccountSnapshot[],
  currency: Currency
}>();

const {t} = useI18n();

const formatDate = (dateString: string) => {
  try {
    return format(parseISO(dateString), 'dd.MM.yyyy');
  } catch (error) {
    return '';
  }
};
</script>

<template>
  <UCard>
    <template #header>
      <div class="flex items-center justify-between">
        <h3 class="text-base font-semibold text-gray-900 dark:text-white">
          {{ t('accounts.dashboard.accountHistory') }}
        </h3>
      </div>
    </template>

    <div class="overflow-y-auto max-h-64">
      <ul class="divide-y divide-neutral-200 dark:divide-neutral-800">
        <li v-for="snapshot in snapshots"
            :key="snapshot.createdAt"
            class="py-2 sm:py-3 flex justify-between items-center">
          <span class="text-sm text-neutral-500 dark:text-neutral-400">
            {{ formatDate(snapshot.createdAt) }}
          </span>
          <span class="text-sm font-medium text-gray-900 dark:text-white">
            <BalanceNumberFormat :balance="snapshot.balance"
                                 :currency="currency"/>
          </span>
        </li>
      </ul>
    </div>
  </UCard>
</template>
