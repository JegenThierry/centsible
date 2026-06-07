<script lang="ts" setup>
import {h, resolveComponent} from 'vue';
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
  repayments: [loan: Loan];
  edit: [loan: Loan];
  delete: [loan: Loan];
  openContact: [loan: Loan];
}>();

const UButton = resolveComponent('UButton');
const {t, moneyColumn, dateColumn, descriptionColumn, outstandingColumn, statusColumn} = useLoanColumns();

const contactColumn: TableColumn<Loan> = {
  accessorKey: 'contact',
  header: t('contacts.loansPage.table.contact'),
  cell: ({row}) => {
    const name = row.original.contact?.name?.trim() || '—';
    return h(
      UButton,
      {
        variant: 'link',
        color: 'neutral',
        class: 'p-0',
        onClick: () => emit('openContact', row.original),
      },
      () => name,
    );
  },
};

const columns = computed<TableColumn<Loan>[]>(() => [
  contactColumn,
  dateColumn,
  descriptionColumn,
  moneyColumn('lentAmount', 'contacts.loans.table.lent', 'font-medium'),
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
          label: t('contacts.loansPage.openContact'),
          icon: 'i-lucide-user',
          onSelect: () => emit('openContact', row.original),
        },
        {
          label: t('contacts.loans.table.recordRepayment'),
          icon: 'i-lucide-hand-helping',
          disabled: Number(row.original.outstanding) <= 0,
          onSelect: () => emit('repay', row.original),
        },
        {
          label: t('contacts.loans.repayments.viewAction'),
          icon: 'i-lucide-history',
          disabled: Number(row.original.totalRepaid) <= 0,
          onSelect: () => emit('repayments', row.original),
        },
        {
          label: t('contacts.loans.table.editLoan'),
          icon: 'i-lucide-pencil',
          onSelect: () => emit('edit', row.original),
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
  <BaseTable :columns="columns" :data="loans" :loading="loading" :empty-title="t('contacts.loansPage.empty')"/>
</template>
