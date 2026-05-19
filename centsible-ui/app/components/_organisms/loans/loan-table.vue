<script lang="ts" setup>
import {h} from 'vue';
import type {TableColumn} from '@nuxt/ui';
import type {Loan} from "~/models/loan/loan";
import BaseTable from "~/components/_molecules/tables/base-table.vue";
import TableRowActionsMenu from "~/components/_molecules/tables/table-row-actions-menu.vue";
import {useLoanColumns} from "~/composables/use-loan-columns";

defineProps<{
  loans: Loan[];
  loading?: boolean;
}>();

const emit = defineEmits<{
  repay: [loan: Loan];
  delete: [loan: Loan];
}>();

const {t, moneyColumn, dateColumn, descriptionColumn, outstandingColumn, statusColumn} = useLoanColumns();

const columns = computed<TableColumn<Loan>[]>(() => [
  dateColumn,
  descriptionColumn,
  moneyColumn('lentAmount', 'contacts.loans.table.lent', 'font-medium'),
  moneyColumn('owedAmount', 'contacts.loans.table.owed', 'font-medium'),
  moneyColumn('totalRepaid', 'contacts.loans.table.repaid', 'text-success'),
  outstandingColumn,
  statusColumn,
  {
    id: 'actions',
    meta: {class: {td: 'text-right'}},
    cell: ({row}) => h(TableRowActionsMenu, {
      menuLabel: t('contacts.loans.table.actionsAria'),
      items: [
        {
          label: t('contacts.loans.table.recordRepayment'),
          icon: 'i-lucide-hand-helping',
          disabled: Number(row.original.outstanding) <= 0,
          onSelect: () => emit('repay', row.original),
        },
        {
          label: t('contacts.loans.table.deleteLoan'),
          icon: 'i-lucide-trash',
          color: 'error' as any,
          onSelect: () => emit('delete', row.original),
        },
      ],
    }),
  },
]);
</script>

<template>
  <BaseTable :columns="columns" :data="loans" :loading="loading" :empty-title="t('contacts.loans.table.empty')"/>
</template>
