<script lang="ts" setup>
import {type Loan, loanStatus} from "~/models/loan/loan";

const props = defineProps<{
  loan: Loan;
}>();

const {t} = useI18n();

const presentation = computed(() => {
  switch (loanStatus(props.loan)) {
    case 'settled':
      return {color: 'success' as const, key: 'contacts.loans.table.statusSettled'};
    case 'partial':
      return {color: 'warning' as const, key: 'contacts.loans.table.statusPartial'};
    case 'open':
      return {color: 'neutral' as const, key: 'contacts.loans.table.statusOpen'};
  }
});
</script>

<template>
  <UBadge :color="presentation.color" size="sm" variant="subtle">
    {{ t(presentation.key) }}
  </UBadge>
</template>
