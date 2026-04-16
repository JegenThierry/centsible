<script lang="ts" setup>
import type {Currency} from "~/models/budget-account/currency";
import BalanceNumberFormat from "~/components/_molecules/labels/balance-number-format.vue";
import BalanceChangeBadge from "~/components/_molecules/badges/balance-change-badge.vue";

const props = defineProps<{
  accountName: string,
  balance: number,
  initialBalance: number,
  currency: Currency,
}>();
</script>

<template>
  <UCard>
    <div class="relative z-10 p-4 sm:p-7 space-y-4 sm:space-y-6">
      <div class="flex items-start justify-between">
        <div class="space-y-1">
          <h2 class="text-base sm:text-lg font-semibold text-gray-900 dark:text-white leading-tight tracking-tight">
            {{ accountName }}
          </h2>
        </div>

        <div class="flex flex-col items-end gap-2">
          <BalanceChangeBadge :currency="currency"
                              :current-balance="balance"
                              :previous-balance="initialBalance"/>
        </div>
      </div>

      <div class="space-y-1">
        <p class="text-xs tracking-widest uppercase text-neutral-500 dark:text-neutral-400 font-medium">
          Available Balance
        </p>
        <div class="flex items-end gap-1">
          <span
            class="text-3xl sm:text-5xl font-bold tracking-tight text-gray-900 dark:text-white tabular-nums leading-none">
            <BalanceNumberFormat :balance="balance"
                                 :currency="currency"
                                 format="de-De"/>
          </span>
        </div>
      </div>
    </div>
  </UCard>
</template>
