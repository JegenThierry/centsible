<script setup lang="ts">
import type {Currency} from "~/models/budget-account/currency";
import BalanceNumberFormat from "~/components/_molecules/labels/balance-number-format.vue";
import {format, parseISO} from 'date-fns';
import type {BudgetAccountSnapshot} from "~/models/budget-account/budget-account";

defineProps<{
  snapshots: BudgetAccountSnapshot[],
  currency: Currency
}>();

const accountHistoryStore = useAccountHistoryStore();

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
        <h3 class="text-base font-semibold text-white">
          Account History
        </h3>
      </div>
    </template>

    <div class="overflow-y-auto max-h-64">
      <ul class="divide-y divide-neutral-800">
        <li v-for="snapshot in accountHistoryStore.snapshots"
            :key="snapshot.createdAt"
            class="py-3 flex justify-between items-center">
          <span class="text-sm text-neutral-400">
            {{ formatDate(snapshot.createdAt) }}
          </span>
          <span class="text-sm font-medium text-white">
            <BalanceNumberFormat :balance="snapshot.balance"
                                 :currency="currency"
                                 format="de-De"/>
          </span>
        </li>
      </ul>
    </div>
  </UCard>
</template>
