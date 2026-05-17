<script lang="ts" setup>
import {computed, h, ref} from 'vue'
import type {TableColumn} from '@nuxt/ui'
import {useIntersectionObserver} from '@vueuse/core'
import {useBudgetAccountsStore} from "~/stores/budgetAccountsStore";
import {useTransactionService} from "~/services/transactions/transaction-service";
import type {Transaction} from "~/models/transactions/transaction";
import type {TransactionFilters} from "~/models/transactions/transaction-filters";
import {useActiveCurrency} from "~/composables/use-active-currency";
import {useToasts} from "~/services/toasts/toast-service";
import TransactionAmount from "~/components/_molecules/transactions/transaction-amount.vue";
import EditTransactionModal from "~/components/_organisms/transactions/modals/edit-transaction-modal.vue";
import DeleteTransactionModal from "~/components/_organisms/transactions/modals/delete-transaction-modal.vue";
import BulkCategorizeModal from "~/components/_organisms/transactions/modals/bulk-categorize-modal.vue";
import CreateFab from "~/components/_molecules/buttons/create-fab.vue";
import CreateTransactionModal from "~/components/_organisms/transactions/modals/create-transaction-modal.vue";
import {useTransactionList} from "~/components/_organisms/transactions/utils/use-transaction-list";
import LoadingAnimation from "~/components/_atoms/animations/loading-animation.vue";
import CategoryBadge from "~/components/_molecules/badges/category-badge.vue";
import FormattedDate from "~/components/_atoms/labels/formatted-date.vue";
import BaseTable from "~/components/_molecules/tables/base-table.vue";
import TableRowActionsMenu from "~/components/_molecules/tables/table-row-actions-menu.vue";
import TransactionFilterBar from "~/components/_molecules/transactions/transaction-filter-bar.vue";

const api = useApi();
const transactionService = useTransactionService(api);
const budgetAccountsStore = useBudgetAccountsStore();
const currency = useActiveCurrency();
const toasts = useToasts();
const {t} = useI18n();

const filters = ref<TransactionFilters>({sort: 'DATE_DESC'});

const {
  transactions,
  loading,
  loadingMore,
  hasMore,
  loadTransactions
} = useTransactionList(transactionService, budgetAccountsStore, 25, filters);

const loadMoreTrigger = ref<HTMLElement | null>(null)

const isCreateModalOpen = ref(false);
const isEditModalOpen = ref(false);
const isDeleteModalOpen = ref(false);
const isBulkCategorizeOpen = ref(false);
const selectedTransaction = ref<Transaction | null>(null);
const selectedIds = ref<Set<string>>(new Set());

function openEditModal(transaction: Transaction) {
  selectedTransaction.value = transaction;
  isEditModalOpen.value = true;
}

function openDeleteModal(transaction: Transaction) {
  selectedTransaction.value = transaction;
  isDeleteModalOpen.value = true;
}

const allOnPageSelected = computed(() =>
  transactions.value.length > 0 && transactions.value.every((t) => selectedIds.value.has(t.id))
);

function toggleAll(checked: boolean) {
  const next = new Set(selectedIds.value);
  if (checked) transactions.value.forEach((t) => next.add(t.id));
  else transactions.value.forEach((t) => next.delete(t.id));
  selectedIds.value = next;
}

function toggleOne(id: string, checked: boolean) {
  const next = new Set(selectedIds.value);
  if (checked) next.add(id); else next.delete(id);
  selectedIds.value = next;
}

function clearSelection() {
  selectedIds.value = new Set();
}

const columns = computed<TableColumn<Transaction>[]>(() => [
  {
    id: 'select',
    header: () => h('input', {
      type: 'checkbox',
      'aria-label': t('transactions.bulk.selectAll'),
      checked: allOnPageSelected.value,
      class: 'cursor-pointer',
      onChange: (e: Event) => toggleAll((e.target as HTMLInputElement).checked),
    }),
    cell: ({row}) => h('input', {
      type: 'checkbox',
      'aria-label': t('transactions.bulk.selectRow'),
      checked: selectedIds.value.has(row.original.id),
      class: 'cursor-pointer',
      onChange: (e: Event) => toggleOne(row.original.id, (e.target as HTMLInputElement).checked),
    }),
  },
  {
    accessorKey: 'transactionDate',
    header: t('transactions.table.date'),
    cell: ({row}) => {
      return h(FormattedDate, {
        date: row.getValue('transactionDate'),
        format: 'full'
      })
    }
  },
  {
    accessorKey: 'description',
    header: t('transactions.table.description')
  },
  {
    accessorKey: 'category',
    header: t('transactions.table.category'),
    cell: ({row}) => {
      const category = row.getValue('category') as any
      return h(CategoryBadge, {
        name: category?.name,
        icon: category?.icon,
        color: category?.color
      })
    }
  },
  {
    accessorKey: 'amount',
    header: t('transactions.table.amount'),
    meta: {
      class: {
        th: 'text-right',
        td: 'text-right font-medium'
      }
    },
    cell: ({row}) => {
      return h(TransactionAmount, {
        amount: Number.parseFloat(row.getValue('amount')),
        type: row.original.category?.type,
        currency: currency.value,
      })
    }
  },
  {
    id: 'actions',
    meta: {
      class: {
        td: 'text-right'
      }
    },
    cell: ({row}) => h(TableRowActionsMenu, {
      menuLabel: t('transactions.table.actionsLabel'),
      items: [
        {
          label: t('transactions.table.actionEdit'),
          icon: 'i-lucide-pencil',
          onSelect: () => openEditModal(row.original)
        },
        {
          label: t('transactions.table.actionDelete'),
          icon: 'i-lucide-trash',
          color: 'error' as any,
          onSelect: () => openDeleteModal(row.original)
        }
      ],
    })
  }
])

async function bulkDelete() {
  const ids = Array.from(selectedIds.value);
  if (ids.length === 0 || !budgetAccountsStore.activeAccount) return;
  if (!confirm(t('transactions.bulk.deleteConfirm', {count: ids.length}))) return;
  try {
    await transactionService.bulkDelete(budgetAccountsStore.activeAccount.id, ids);
    toasts.success(t('transactions.bulk.deleteToastTitle'), t('transactions.bulk.deleteToastBody', {count: ids.length}));
    clearSelection();
    await budgetAccountsStore.updateActiveAccount();
    await loadTransactions(true);
  } catch (e) {
    toasts.error(t('transactions.bulk.errorTitle'), t('transactions.bulk.errorBody'));
  }
}

async function bulkCategorize(categoryId: number) {
  const ids = Array.from(selectedIds.value);
  if (ids.length === 0 || !budgetAccountsStore.activeAccount) return;
  try {
    await transactionService.bulkCategorize(budgetAccountsStore.activeAccount.id, ids, categoryId);
    toasts.success(t('transactions.bulk.recategorizeToastTitle'), t('transactions.bulk.recategorizeToastBody', {count: ids.length}));
    clearSelection();
    await budgetAccountsStore.updateActiveAccount();
    await loadTransactions(true);
  } catch (e) {
    toasts.error(t('transactions.bulk.errorTitle'), t('transactions.bulk.errorBody'));
  }
}

useIntersectionObserver(loadMoreTrigger, async (entries) => {
  const entry = entries[0]
  if (!entry?.isIntersecting) return
  if (loading.value || loadingMore.value || !hasMore.value) return

  await loadTransactions()
})

watch(
  () => budgetAccountsStore.activeAccount?.id,
  (id) => {
    if (id) {
      clearSelection();
      loadTransactions(true);
    }
  },
  {immediate: true},
)
</script>

<template>
  <TransactionFilterBar v-model="filters"/>

  <div v-if="selectedIds.size > 0"
       class="flex items-center gap-2 mb-3 p-3 rounded-lg bg-primary-50 dark:bg-primary-900/20 border border-primary-200 dark:border-primary-800">
    <span class="text-sm font-medium">{{ t('transactions.bulk.selectedCount', {count: selectedIds.size}) }}</span>
    <div class="ml-auto flex gap-2">
      <UButton color="primary" icon="i-lucide-tag" size="sm" variant="outline" @click="isBulkCategorizeOpen = true">
        {{ t('transactions.bulk.recategorize') }}
      </UButton>
      <UButton color="error" icon="i-lucide-trash" size="sm" variant="outline" @click="bulkDelete">
        {{ t('transactions.bulk.delete') }}
      </UButton>
      <UButton color="neutral" size="sm" variant="ghost" @click="clearSelection">
        {{ t('transactions.bulk.clear') }}
      </UButton>
    </div>
  </div>

  <BaseTable :columns="columns"
             :data="transactions"
             :loading="loading"
             :empty-title="t('transactions.emptyTitle')"
             :loading-message="t('transactions.loadingMessage')"
             class="flex-1 overflow-y-auto"/>

  <div v-if="hasMore && transactions.length > 0" ref="loadMoreTrigger" class="flex justify-center p-4">
    <LoadingAnimation v-if="loadingMore || loading"/>
  </div>

  <CreateFab @create="isCreateModalOpen = true"/>

  <CreateTransactionModal v-if="isCreateModalOpen"
                          v-model:open="isCreateModalOpen"
                          @created="loadTransactions(true)"/>

  <EditTransactionModal v-if="isEditModalOpen && selectedTransaction !== null"
                        v-model:open="isEditModalOpen"
                        :transaction="selectedTransaction"
                        @updated="loadTransactions(true)"/>

  <DeleteTransactionModal v-if="isDeleteModalOpen"
                          v-model:open="isDeleteModalOpen"
                          :transaction="selectedTransaction"
                          @deleted="loadTransactions(true)"/>

  <BulkCategorizeModal v-if="isBulkCategorizeOpen"
                       v-model:open="isBulkCategorizeOpen"
                       :count="selectedIds.size"
                       @confirm="bulkCategorize"/>
</template>
