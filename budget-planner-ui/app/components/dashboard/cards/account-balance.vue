<script setup lang="ts">
import type {Currency} from "~/models/budget-account/currency";
import BalanceNumberFormat from "~/components/_molecules/labels/balance-number-format.vue";
import BalanceChangeBadge from "~/components/_molecules/badges/balance-change-badge.vue";

defineProps<{
  accountName: string,
  balance: number,
  initialBalance: number,
  currency: Currency,
}>();
</script>

<template>
  <UCard>
    <div class="relative z-10 p-7 space-y-6">
      <div class="flex items-start justify-between">
        <div class="space-y-1">
          <h2 class="text-lg font-semibold text-gray-900 dark:text-white leading-tight tracking-tight">
            {{ accountName }}
          </h2>
        </div>

        <BalanceChangeBadge :current-balance="balance"
                            :previous-balance="initialBalance"
                            :currency="currency"/>
      </div>

      <div class="space-y-1">
        <p class="text-xs tracking-widest uppercase text-neutral-500 dark:text-neutral-400 font-medium">
          Available Balance
        </p>
        <div class="flex items-end gap-1">
          <span class="text-5xl font-bold tracking-tight text-gray-900 dark:text-white tabular-nums leading-none">
            <BalanceNumberFormat :currency="currency"
                                 :balance="balance"
                                 format="de-De"/>
          </span>
        </div>
      </div>
    </div>
  </UCard>
</template>
