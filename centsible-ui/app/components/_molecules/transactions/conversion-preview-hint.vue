<script lang="ts" setup>
import type {Currency} from "~/models/budget-account/currency";
import BalanceNumberFormat from "~/components/_atoms/labels/balance-number-format.vue";

defineProps<{
  /** Whether the entered amount is in a foreign currency — drives visibility. */
  foreign: boolean;
  /** Entered amount; the hint only renders for a positive amount. */
  amount: number;
  /** Converted amount in the target currency, or null/undefined while pending. */
  converted: number | null | undefined;
  /** True when the conversion failed (no rate available). */
  failed: boolean;
  /** Target (account) currency the converted amount is shown in. */
  currency: Currency | undefined;
}>();

const {t} = useI18n();
</script>

<template>
  <p v-if="foreign && amount > 0" class="-mt-2 px-1 text-xs text-muted">
    <span v-if="converted != null && currency">
      ≈ <BalanceNumberFormat :balance="converted" :currency="currency"/>
    </span>
    <span v-else-if="failed">{{ t('transactions.form.conversionUnavailable') }}</span>
    <span v-else>≈ …</span>
  </p>
</template>
