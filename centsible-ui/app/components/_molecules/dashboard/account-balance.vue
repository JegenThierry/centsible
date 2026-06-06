<script lang="ts" setup>
import type {Currency} from "~/models/budget-account/currency";
import BalanceNumberFormat from "~/components/_atoms/labels/balance-number-format.vue";

const props = defineProps<{
  accountName: string,
  balance: number,
  initialBalance: number,
  currency: Currency,
}>();

const {t} = useI18n();

const balanceChange = computed(() => props.balance - props.initialBalance);
const isUp = computed(() => balanceChange.value >= 0);
</script>

<template>
  <UCard class="overflow-hidden ring-0"
         :ui="{ body: 'relative h-full bg-gradient-to-br from-primary-700 via-primary-800 to-neutral-800 text-white' }">
    <div class="pointer-events-none absolute -top-12 -right-12 h-44 w-44 rounded-full bg-white/10 blur-2xl"/>

    <div class="relative z-10 flex h-full flex-col gap-6">
      <div class="flex items-start justify-between gap-2">
        <div class="h-7 w-9 rounded-md bg-gradient-to-br from-amber-200 to-amber-400 ring-1 ring-white/30"/>
        <span class="text-xs font-semibold uppercase tracking-widest text-white/80 text-right">
          {{ accountName }}
        </span>
      </div>

      <div class="flex flex-1 flex-col justify-center space-y-1">
        <p class="text-[11px] uppercase tracking-widest text-white/70 font-medium">
          {{ t('accounts.dashboard.availableBalance') }}
        </p>
        <div class="flex flex-wrap items-end gap-x-3 gap-y-2">
          <span class="text-3xl sm:text-5xl font-bold tracking-tight tabular-nums leading-none">
            <BalanceNumberFormat :balance="balance"
                                 :currency="currency"/>
          </span>
          <span class="inline-flex items-center gap-1 rounded-full bg-white/15 px-2 py-0.5 text-xs font-semibold tabular-nums">
            <UIcon :name="isUp ? 'i-lucide-trending-up' : 'i-lucide-trending-down'" class="size-3.5"/>
            <BalanceNumberFormat :balance="balanceChange"
                                 :currency="currency"/>
          </span>
        </div>
      </div>

      <div class="flex items-center gap-3 text-white/55">
        <span class="text-lg leading-none tracking-[0.25em]">••••</span>
        <span class="text-lg leading-none tracking-[0.25em]">••••</span>
        <span class="text-lg leading-none tracking-[0.25em]">••••</span>
        <span class="ml-auto text-xs font-semibold uppercase tracking-widest text-white/70">
          {{ currency }}
        </span>
      </div>
    </div>
  </UCard>
</template>
