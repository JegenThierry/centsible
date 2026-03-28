<script setup lang="ts">
import type {Currency} from "~/models/budget-account/currency";

const props = defineProps<{
  accountName: string,
  balance: number,
  currency: Currency,
}>();

const formattedBalance = computed(() => {
  return new Intl.NumberFormat('de-DE', {
    style: 'currency',
    currency: props.currency.toString(),
    minimumFractionDigits: 2,
  }).format(props.balance)
})

const [whole, cents] = formattedBalance.value.split(/(?<=\d)(?=\D*$)/)
</script>

<template>
  <UCard>
    <div class="relative z-10 p-7 space-y-6">

      <div class="flex items-start justify-between">
        <div class="space-y-1">
          <h2 class="text-lg font-semibold text-white leading-tight tracking-tight">
            {{ accountName }}
          </h2>
        </div>

        <UBadge
            :color="balance > 0 ? 'success' : 'error'"
            variant="subtle"
            size="sm"
            class="mt-0.5 font-mono font-semibold"
        >
          <UIcon
              :name="balance > 0 ? 'i-lucide-trending-up' : 'i-lucide-trending-down'"
              class="mr-1 size-3.5"
          />
          {{balance}}
        </UBadge>
      </div>

      <div class="space-y-1">
        <p class="text-xs tracking-widest uppercase text-neutral-500 font-medium">
          Available Balance
        </p>
        <div class="flex items-end gap-1">
          <span class="text-5xl font-bold tracking-tight text-white tabular-nums leading-none">
            {{ whole }}
          </span>
          <span class="text-2xl font-semibold text-neutral-400 mb-0.5 tabular-nums leading-none">
            {{ cents }}
          </span>
        </div>
      </div>

      <USeparator class="border-white/10"/>
    </div>
  </UCard>
</template>
