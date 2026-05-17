<script lang="ts" setup>
import {h} from 'vue'
import type {TableColumn} from '@nuxt/ui'
import {useIntersectionObserver} from '@vueuse/core'
import {useBudgetAccountsStore} from "~/stores/budgetAccountsStore";
import {useTransactionService} from "~/services/transactions/transaction-service";
import type {Transaction} from "~/models/transactions/transaction";
import {useActiveCurrency} from "~/composables/use-active-currency";
import TransactionAmount from "~/components/_molecules/transactions/transaction-amount.vue";
import EditTransactionModal from "~/components/_organisms/transactions/modals/edit-transaction-modal.vue";
import DeleteTransactionModal from "~/components/_organisms/transactions/modals/delete-transaction-modal.vue";
import CreateFab from "~/components/_molecules/buttons/create-fab.vue";
import CreateTransactionModal from "~/components/_organisms/transactions/modals/create-transaction-modal.vue";
import {useTransactionList} from "~/components/_organisms/transactions/utils/use-transaction-list";
import LoadingAnimation from "~/components/_atoms/animations/loading-animation.vue";
import CategoryBadge from "~/components/_molecules/badges/category-badge.vue";
import FormattedDate from "~/components/_atoms/labels/formatted-date.vue";
import BaseTable from "~/components/_molecules/tables/base-table.vue";
import TableRowActionsMenu from "~/components/_molecules/tables/table-row-actions-menu.vue";

const api = useApi();
const transactionService = useTransactionService(api);
const budgetAccountsStore = useBudgetAccountsStore();
const currency = useActiveCurrency();
const {t} = useI18n();

const {
  transactions,
  loading,
  loadingMore,
  hasMore,
  loadTransactions
} = useTransactionList(transactionService, budgetAccountsStore);

const loadMoreTrigger = ref<HTMLElement | null>(null)

const isCreateModalOpen = ref(false);
const isEditModalOpen = ref(false);
const isDeleteModalOpen = ref(false);
const selectedTransaction = ref<Transaction | null>(null);

function openEditModal(transaction: Transaction) {
  selectedTransaction.value = transaction;
  isEditModalOpen.value = true;
}

function openDeleteModal(transaction: Transaction) {
  selectedTransaction.value = transaction;
  isDeleteModalOpen.value = true;
}

const columns = computed<TableColumn<Transaction>[]>(() => [
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

useIntersectionObserver(loadMoreTrigger, async (entries) => {
  const entry = entries[0]
  if (!entry?.isIntersecting) return
  if (loading.value || loadingMore.value || !hasMore.value) return

  await loadTransactions()
})

watch(
  () => budgetAccountsStore.activeAccount?.id,
  (id) => {
    if (id) loadTransactions(true)
  },
  {immediate: true},
)
</script>

<template>
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
</template>
