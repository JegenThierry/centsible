<script lang="ts" setup>
import {type Loan, loanStatus} from "~/models/loan/loan";

const props = defineProps<{
  loan: Loan;
}>();

const {t} = useI18n();

const loanStatusToPresentation = {
  settled: {color: 'success' as const, key: 'contacts.loans.table.statusSettled'},
  partial: {color: 'warning' as const, key: 'contacts.loans.table.statusPartial'},
  open: {color: 'neutral' as const, key: 'contacts.loans.table.statusOpen'},
};

const presentation = computed(() => loanStatusToPresentation[loanStatus(props.loan)]);
</script>

<template>
  <UBadge :color="presentation.color" size="sm" variant="subtle">
    {{ t(presentation.key) }}
  </UBadge>
</template>
