import {h, resolveComponent} from 'vue';
import type {TableColumn} from '@nuxt/ui';
import type {Loan} from "~/models/loan/loan";
import {Currency} from "~/models/budget-account/currency";
import BalanceNumberFormat from "~/components/_atoms/labels/balance-number-format.vue";
import FormattedDate from "~/components/_atoms/labels/formatted-date.vue";
import LoanStatusBadge from "~/components/_atoms/loans/loan-status-badge.vue";

/**
 * Shared column builders for loan tables. Each table composes a different subset; this composable
 * is the single source of truth for cell rendering so visual styling stays in lockstep.
 */
export function useLoanColumns() {
  const {t} = useI18n();
  const UBadge = resolveComponent('UBadge');

  // Loans can each be in a different currency, so cells render in the loan's own currency.
  function balanceCell(value: number, currency: Currency) {
    return h(BalanceNumberFormat, {balance: value, currency, format: 'de-De'});
  }

  function moneyColumn(key: keyof Loan, headerKey: string, tdClass: string): TableColumn<Loan> {
    return {
      accessorKey: key,
      header: t(headerKey),
      meta: {class: {th: 'text-right', td: `text-right ${tdClass}`}},
      cell: ({row}) => balanceCell(Number(row.original[key]), row.original.currency ?? Currency.EUR),
    };
  }

  const dateColumn: TableColumn<Loan> = {
    accessorKey: 'loanDate',
    header: t('contacts.loans.table.date'),
    cell: ({row}) => {
      const date = row.getValue('loanDate') as string | undefined;
      if (!date) return '—';
      return h(FormattedDate, {date, format: 'date'});
    },
  };

  const descriptionColumn: TableColumn<Loan> = {
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
  };

  const outstandingColumn: TableColumn<Loan> = {
    accessorKey: 'outstanding',
    header: t('contacts.loans.table.outstanding'),
    meta: {class: {th: 'text-right', td: 'text-right font-semibold'}},
    cell: ({row}) => {
      const value = Number(row.original.outstanding);
      const cls = value > 0 ? 'text-warning' : 'text-muted';
      return h('span', {class: cls}, [balanceCell(value, row.original.currency ?? Currency.EUR)]);
    },
  };

  const statusColumn: TableColumn<Loan> = {
    accessorKey: 'status',
    header: t('contacts.loans.table.status'),
    cell: ({row}) => h(LoanStatusBadge, {loan: row.original}),
  };

  return {
    t,
    balanceCell,
    moneyColumn,
    dateColumn,
    descriptionColumn,
    outstandingColumn,
    statusColumn,
  };
}
