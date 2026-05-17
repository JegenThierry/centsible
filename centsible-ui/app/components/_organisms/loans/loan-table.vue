<script lang="ts" setup>
import {h, resolveComponent} from 'vue';
import type {TableColumn} from '@nuxt/ui';
import type {Loan} from "~/models/loan/loan";
import BalanceNumberFormat from "~/components/_atoms/labels/balance-number-format.vue";
import FormattedDate from "~/components/_atoms/labels/formatted-date.vue";
import BaseTable from "~/components/_molecules/tables/base-table.vue";
import TableRowActionsMenu from "~/components/_molecules/tables/table-row-actions-menu.vue";
import {useActiveCurrency} from "~/composables/use-active-currency";

defineProps<{
  loans: Loan[];
  loading?: boolean;
}>();

const emit = defineEmits<{
  repay: [loan: Loan];
  delete: [loan: Loan];
}>();

const UBadge = resolveComponent('UBadge');

const {t} = useI18n();
const currency = useActiveCurrency();

type LoanStatus = 'settled' | 'partial' | 'open';
const STATUS_BADGE: Record<LoanStatus, {color: 'success' | 'warning' | 'neutral'; key: string}> = {
  settled: {color: 'success', key: 'contacts.loans.table.statusSettled'},
  partial: {color: 'warning', key: 'contacts.loans.table.statusPartial'},
  open: {color: 'neutral', key: 'contacts.loans.table.statusOpen'},
};

function loanStatus(loan: Loan): LoanStatus {
  if (Number(loan.outstanding) <= 0) return 'settled';
  if (Number(loan.totalRepaid) > 0) return 'partial';
  return 'open';
}

function balanceCell(value: number) {
  return h(BalanceNumberFormat, {balance: value, currency: currency.value, format: 'de-De'});
}

function moneyColumn(key: keyof Loan, headerKey: string, tdClass: string): TableColumn<Loan> {
  return {
    accessorKey: key,
    header: t(headerKey),
    meta: {class: {th: 'text-right', td: `text-right ${tdClass}`}},
    cell: ({row}) => balanceCell(Number(row.original[key])),
  };
}

const columns = computed<TableColumn<Loan>[]>(() => [
  {
    accessorKey: 'loanDate',
    header: t('contacts.loans.table.date'),
    cell: ({row}) => {
      const date = row.getValue('loanDate') as string | undefined;
      if (!date) return '—';
      return h(FormattedDate, {date, format: 'date'});
    },
  },
  {
    accessorKey: 'description',
    header: t('contacts.loans.table.description'),
    cell: ({row}) => {
      const text = row.original.description || '—';
      if (row.original.affectsBalance) return text;
      return h('div', {class: 'flex items-center gap-2'}, [
        h('span', text),
        h(UBadge, {color: 'neutral', variant: 'subtle', size: 'xs'}, () => t('contacts.loans.table.trackingOnly')),
      ]);
    },
  },
  moneyColumn('lentAmount', 'contacts.loans.table.lent', 'font-medium'),
  moneyColumn('owedAmount', 'contacts.loans.table.owed', 'font-medium'),
  moneyColumn('totalRepaid', 'contacts.loans.table.repaid', 'text-success'),
  {
    accessorKey: 'outstanding',
    header: t('contacts.loans.table.outstanding'),
    meta: {class: {th: 'text-right', td: 'text-right font-semibold'}},
    cell: ({row}) => {
      const value = Number(row.original.outstanding);
      return h('span', {class: value > 0 ? 'text-warning' : 'text-muted'}, [balanceCell(value)]);
    },
  },
  {
    accessorKey: 'status',
    header: t('contacts.loans.table.status'),
    cell: ({row}) => {
      const meta = STATUS_BADGE[loanStatus(row.original)];
      return h(UBadge, {color: meta.color, variant: 'subtle', size: 'sm'}, () => t(meta.key));
    },
  },
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
