import {h} from 'vue'
import type {TableColumn} from '@nuxt/ui'
import type {Transaction} from '~/models/transactions/transaction'
import type {Category} from '~/models/category/category'
import type {Currency} from '~/models/budget-account/currency'
import {CategoryType} from '~/models/category/category'
import {transactionType} from '~/utils/transaction'
import TransactionAmount from '~/components/_molecules/transactions/transaction-amount.vue'
import TransactionAttachmentsPopover from '~/components/_organisms/transactions/transaction-attachments-popover.vue'
import CategoryBadge from '~/components/_molecules/badges/category-badge.vue'
import InlineCategoryPicker from '~/components/_molecules/transactions/inline-category-picker.vue'
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
  categories: () => Category[]
  isSelected: (id: string) => boolean
  isAllSelected: () => boolean
  onToggleAll: (checked: boolean) => void
  onToggleOne: (id: string, checked: boolean) => void
  onEdit: (transaction: Transaction) => void
  onDelete: (transaction: Transaction) => void
  onCreateRule: (transaction: Transaction) => void
  onSplitIntoIous: (transaction: Transaction) => void
  onMakeRecurring: (transaction: Transaction) => void
  onCategorize: (transaction: Transaction, category: Category) => void
}

export function createTransactionColumns(options: TransactionColumnsOptions): TableColumn<Transaction>[] {
  const {t, currency, categories, isSelected, isAllSelected, onToggleAll, onToggleOne, onEdit, onDelete, onCreateRule, onSplitIntoIous, onMakeRecurring, onCategorize} = options

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
      cell: ({row}) => {
        const tags = row.original.tags ?? []
        const description = h('span', row.original.description)
        if (tags.length === 0) return description
        return h('div', {class: 'flex flex-col gap-1'}, [
          description,
          h('div', {class: 'flex flex-wrap gap-1'}, tags.map((tag) => h('span', {
            key: tag.id,
            class: 'inline-flex items-center gap-1 rounded-full border border-default px-1.5 py-0.5 text-[10px] leading-none text-muted',
          }, [
            h('span', {class: 'w-1.5 h-1.5 rounded-full shrink-0', style: {backgroundColor: tag.color}}),
            tag.name,
          ]))),
        ])
      },
    },
    {
      accessorKey: 'category',
      header: () => t('transactions.table.category'),
      cell: ({row}) => {
        const splits = row.original.splits ?? []
        if (splits.length > 0) {
          return h('div', {class: 'flex flex-col gap-1'}, [
            h('span', {class: 'inline-flex items-center gap-1 text-[10px] font-medium uppercase tracking-wide text-muted'}, [
              h('span', {class: 'i-lucide-split w-3 h-3'}),
              t('transactions.table.splitBadge', {count: splits.length}),
            ]),
            h('div', {class: 'flex flex-wrap gap-1'}, splits.map((split, i) => h(CategoryBadge, {
              key: i,
              name: split.category?.name,
              icon: split.category?.icon,
              color: split.category?.color,
            }))),
          ])
        }
        const category = row.original.category
        if (row.original.transferGroupId) {
          return h(CategoryBadge, {
            name: category?.name,
            icon: category?.icon,
            color: category?.color,
          })
        }
        return h(InlineCategoryPicker, {
          category,
          options: categories(),
          onSelect: (picked: Category) => onCategorize(row.original, picked),
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
        originalAmount: row.original.originalAmount,
        originalCurrency: row.original.originalCurrency,
      }),
    },
    {
      id: 'actions',
      meta: {class: {td: 'text-right'}},
      cell: ({row}) => {
        const canSplit = transactionType(row.original) === CategoryType.EXPENSE && !row.original.transferGroupId
        const items = [
          {
            label: t('transactions.table.actionEdit'),
            icon: 'i-lucide-pencil',
            onSelect: () => onEdit(row.original),
          },
          ...(canSplit ? [{
            label: t('transactions.table.actionSplitIous'),
            icon: 'i-lucide-users',
            onSelect: () => onSplitIntoIous(row.original),
          }] : []),
          ...(!row.original.transferGroupId ? [{
            label: t('transactions.table.actionMakeRecurring'),
            icon: 'i-lucide-repeat',
            onSelect: () => onMakeRecurring(row.original),
          }] : []),
          {
            label: t('transactions.table.actionCreateRule'),
            icon: 'i-lucide-wand-sparkles',
            onSelect: () => onCreateRule(row.original),
          },
          {
            label: t('transactions.table.actionDelete'),
            icon: 'i-lucide-trash',
            color: 'error' as any,
            onSelect: () => onDelete(row.original),
          },
        ]
        return h(TableRowActionsMenu, {
          menuLabel: t('transactions.table.actionsLabel'),
          items,
        })
      },
    },
  ]
}
