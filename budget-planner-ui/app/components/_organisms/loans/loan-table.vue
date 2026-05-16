<script lang="ts" setup>
import {h, resolveComponent} from 'vue';
import type {TableColumn} from '@nuxt/ui';
import type {Loan} from "~/models/loan/loan";
import {Currency} from "~/models/budget-account/currency";
import {useBudgetAccountsStore} from "~/stores/budgetAccountsStore";
import BalanceNumberFormat from "~/components/_molecules/labels/balance-number-format.vue";
import FormattedDate from "~/components/_atoms/labels/formatted-date.vue";
import BaseTable from "~/components/_molecules/tables/base-table.vue";
import TableRowActionsMenu from "~/components/_molecules/tables/table-row-actions-menu.vue";

defineProps<{
  loans: Loan[];
  loading?: boolean;
}>();

const emit = defineEmits<{
  repay: [loan: Loan];
  delete: [loan: Loan];
}>();

const UBadge = resolveComponent('UBadge');

const budgetAccountsStore = useBudgetAccountsStore();
const currency = computed(() => budgetAccountsStore.activeAccount?.currency ?? Currency.EUR);

const columns: TableColumn<Loan>[] = [
  {
    accessorKey: 'loanDate',
    header: 'Date',
    cell: ({row}) => {
      const date = row.getValue('loanDate') as string | undefined;
      if (!date) return '—';
      return h(FormattedDate, {date, format: 'date'});
    },
  },
  {
    accessorKey: 'description',
    header: 'Description',
    cell: ({row}) => {
      const text = row.original.description || '—';
      if (row.original.affectsBalance) return text;
      return h('div', {class: 'flex items-center gap-2'}, [
        h('span', text),
        h(UBadge, {color: 'neutral', variant: 'subtle', size: 'xs'}, () => 'Tracking only'),
      ]);
    },
  },
  {
    accessorKey: 'lentAmount',
    header: 'Lent',
    meta: {class: {th: 'text-right', td: 'text-right font-medium'}},
    cell: ({row}) => h(BalanceNumberFormat, {
      balance: Number(row.original.lentAmount),
      currency: currency.value,
      format: 'de-De',
    }),
  },
  {
    accessorKey: 'owedAmount',
    header: 'Owed',
    meta: {class: {th: 'text-right', td: 'text-right font-medium'}},
    cell: ({row}) => h(BalanceNumberFormat, {
      balance: Number(row.original.owedAmount),
      currency: currency.value,
      format: 'de-De',
    }),
  },
  {
    accessorKey: 'totalRepaid',
    header: 'Repaid',
    meta: {class: {th: 'text-right', td: 'text-right text-success'}},
    cell: ({row}) => h(BalanceNumberFormat, {
      balance: Number(row.original.totalRepaid),
      currency: currency.value,
      format: 'de-De',
    }),
  },
  {
    accessorKey: 'outstanding',
    header: 'Outstanding',
    meta: {class: {th: 'text-right', td: 'text-right font-semibold'}},
    cell: ({row}) => {
      const value = Number(row.original.outstanding);
      const cls = value > 0 ? 'text-warning' : 'text-muted';
      return h('span', {class: cls}, [
        h(BalanceNumberFormat, {
          balance: value,
          currency: currency.value,
          format: 'de-De',
        }),
      ]);
    },
  },
  {
    accessorKey: 'status',
    header: 'Status',
    cell: ({row}) => {
      const outstanding = Number(row.original.outstanding);
      if (outstanding <= 0) {
        return h(UBadge, {color: 'success', variant: 'subtle', size: 'sm'}, () => 'Settled');
      }
      const repaid = Number(row.original.totalRepaid);
      if (repaid > 0) {
        return h(UBadge, {color: 'warning', variant: 'subtle', size: 'sm'}, () => 'Partially paid');
      }
      return h(UBadge, {color: 'neutral', variant: 'subtle', size: 'sm'}, () => 'Open');
    },
  },
  {
    id: 'actions',
    meta: {class: {td: 'text-right'}},
    cell: ({row}) => h(TableRowActionsMenu, {
      label: 'Loan actions',
      items: [
        {
          label: 'Record repayment',
          icon: 'i-lucide-hand-helping',
          disabled: Number(row.original.outstanding) <= 0,
          onSelect: () => emit('repay', row.original),
        },
        {
          label: 'Delete loan',
          icon: 'i-lucide-trash',
          color: 'error' as any,
          onSelect: () => emit('delete', row.original),
        },
      ],
    }),
  },
];
</script>

<template>
  <BaseTable :columns="columns" :data="loans" :loading="loading" empty-title="No loans yet."/>
</template>
