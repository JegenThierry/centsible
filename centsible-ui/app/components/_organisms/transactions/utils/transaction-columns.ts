import {h} from 'vue'
import type {TableColumn} from '@nuxt/ui'
import type {Transaction} from '~/models/transactions/transaction'
import type {Currency} from '~/models/budget-account/currency'
import {transactionType} from '~/utils/transaction'
import TransactionAmount from '~/components/_molecules/transactions/transaction-amount.vue'
import TransactionAttachmentsPopover from '~/components/_organisms/transactions/transaction-attachments-popover.vue'
import CategoryBadge from '~/components/_molecules/badges/category-badge.vue'
import FormattedDate from '~/components/_atoms/labels/formatted-date.vue'
import TableRowActionsMenu from '~/components/_molecules/tables/table-row-actions-menu.vue'

/**
 * Builds the TanStack column definitions for the transactions table.
 *
 * Everything reactive is read lazily through the supplied getters/handlers — TanStack invokes the
 * header/cell thunks on every render, so the closures here mirror the previous in-component
 * behaviour (locale changes surface via {@link t}, selection + currency are read live, row actions
 * are delegated back to the container). The factory owns no state of its own.
 */
export interface TransactionColumnsOptions {
  t: (key: string, ...args: any[]) => string
  currency: () => Currency
  isSelected: (id: string) => boolean
  isAllSelected: () => boolean
  onToggleAll: (checked: boolean) => void
  onToggleOne: (id: string, checked: boolean) => void
  onEdit: (transaction: Transaction) => void
  onDelete: (transaction: Transaction) => void
}

export function createTransactionColumns(options: TransactionColumnsOptions): TableColumn<Transaction>[] {
  const {t, currency, isSelected, isAllSelected, onToggleAll, onToggleOne, onEdit, onDelete} = options

  return [
    {
      id: 'select',
      header: () => h('input', {
        type: 'checkbox',
        'aria-label': t('transactions.bulk.selectAll'),
        checked: isAllSelected(),
        class: 'cursor-pointer',
        onChange: (e: Event) => onToggleAll((e.target as HTMLInputElement).checked),
      }),
      cell: ({row}) => h('input', {
        type: 'checkbox',
        'aria-label': t('transactions.bulk.selectRow'),
        checked: isSelected(row.original.id),
        class: 'cursor-pointer',
        onChange: (e: Event) => onToggleOne(row.original.id, (e.target as HTMLInputElement).checked),
      }),
    },
    {
      accessorKey: 'transactionDate',
      header: () => t('transactions.table.date'),
      cell: ({row}) => h(FormattedDate, {
        date: row.getValue('transactionDate'),
        format: 'full',
      }),
    },
    {
      accessorKey: 'description',
      header: () => t('transactions.table.description'),
    },
    {
      accessorKey: 'category',
      header: () => t('transactions.table.category'),
      cell: ({row}) => {
        const category = row.getValue('category') as any
        return h(CategoryBadge, {
          name: category?.name,
          icon: category?.icon,
          color: category?.color,
        })
      },
    },
    {
      id: 'attachments',
      header: '',
      meta: {class: {th: 'w-10', td: 'w-10'}},
      cell: ({row}) => {
        const count = row.original.attachmentCount ?? 0
        if (count === 0) return null
        return h(TransactionAttachmentsPopover, {
          transactionId: row.original.id,
          count,
        })
      },
    },
    {
      accessorKey: 'amount',
      header: () => t('transactions.table.amount'),
      meta: {class: {th: 'text-right', td: 'text-right font-medium'}},
      cell: ({row}) => h(TransactionAmount, {
        amount: Number.parseFloat(row.getValue('amount')),
        type: transactionType(row.original),
        currency: currency(),
      }),
    },
    {
      id: 'actions',
      meta: {class: {td: 'text-right'}},
      cell: ({row}) => h(TableRowActionsMenu, {
        menuLabel: t('transactions.table.actionsLabel'),
        items: [
          {
            label: t('transactions.table.actionEdit'),
            icon: 'i-lucide-pencil',
            onSelect: () => onEdit(row.original),
          },
          {
            label: t('transactions.table.actionDelete'),
            icon: 'i-lucide-trash',
            color: 'error' as any,
            onSelect: () => onDelete(row.original),
          },
        ],
      }),
    },
  ]
}
