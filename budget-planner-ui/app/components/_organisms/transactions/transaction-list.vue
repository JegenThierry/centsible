<script lang="ts" setup>
import {h, resolveComponent} from 'vue'
import type {TableColumn} from '@nuxt/ui'
import {useIntersectionObserver} from '@vueuse/core'
import {useBudgetAccountsStore} from "~/stores/budgetAccountsStore";
import {useTransactionService} from "~/services/transactions/transaction-service";
import type {Transaction} from "~/models/transactions/transaction";
import {Currency} from "~/models/budget-account/currency";
import TransactionAmount from "~/components/_molecules/transactions/transaction-amount.vue";
import EditTransactionModal from "~/components/_organisms/transactions/modals/edit-transaction-modal.vue";
import DeleteTransactionModal from "~/components/_organisms/transactions/modals/delete-transaction-modal.vue";
import CreateFab from "~/components/_molecules/buttons/create-fab.vue";
import CreateTransactionModal from "~/components/_organisms/transactions/modals/create-transaction-modal.vue";
import {useToasts} from "~/services/toasts/toast-service";
import {useTransactionList} from "~/components/_organisms/transactions/utils/use-transaction-list";
import LoadingAnimation from "~/components/_atoms/animations/loading-animation.vue";

const UButton = resolveComponent('UButton')
const UBadge = resolveComponent('UBadge')
const UIcon = resolveComponent('UIcon')
const UDropdownMenu = resolveComponent('UDropdownMenu')
const CategoryBadge = resolveComponent('CategoryBadge')
const FormattedDate = resolveComponent('FormattedDate')

const api = useApi();
const toast = useToasts();
const transactionService = useTransactionService(api);
const budgetAccountsStore = useBudgetAccountsStore();

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

function onOpenCreateModal() {
  isCreateModalOpen.value = true;
}

function openEditModal(transaction: Transaction) {
  selectedTransaction.value = transaction;
  isEditModalOpen.value = true;
}

function openDeleteModal(transaction: Transaction) {
  selectedTransaction.value = transaction;
  isDeleteModalOpen.value = true;
}

const columns: TableColumn<Transaction>[] = [
  {
    accessorKey: 'transactionDate',
    header: 'Date',
    cell: ({row}) => {
      return h(FormattedDate, {
        date: row.getValue('transactionDate'),
        format: 'full'
      })
    }
  },
  {
    accessorKey: 'description',
    header: 'Description'
  },
  {
    accessorKey: 'category',
    header: 'Category',
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
    header: 'Amount',
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
        currency: budgetAccountsStore.activeAccount?.currency || Currency.EUR
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
    cell: ({row}) => {
      return h(
        UDropdownMenu,
        {
          content: {
            align: 'end'
          },
          items: [
            {
              label: 'Edit',
              icon: 'i-lucide-pencil',
              onSelect: () => openEditModal(row.original)
            },
            {
              label: 'Delete',
              icon: 'i-lucide-trash',
              color: 'error' as any,
              onSelect: () => openDeleteModal(row.original)
            }
          ],
          'aria-label': 'Actions dropdown'
        },
        () =>
          h(UButton, {
            icon: 'i-lucide-ellipsis-vertical',
            color: 'neutral',
            variant: 'ghost',
            'aria-label': 'Actions dropdown'
          })
      )
    }
  }
]

useIntersectionObserver(loadMoreTrigger, async (entries) => {
  const entry = entries[0]
  if (!entry?.isIntersecting) return
  if (loading.value || loadingMore.value || !hasMore.value) return

  await loadTransactions()
})

onMounted(() => {
  if (budgetAccountsStore.activeAccount?.id == undefined) {
    toast.error('No active account selected', 'Please select an account to view transactions.')
  }

  loadTransactions(true)
})
</script>

<template>
  <UCard variant="outline">
    <template #header>
      <div class="flex items-center justify-between">
        <h3 class="font-semibold text-gray-900 dark:text-white">Transactions</h3>
      </div>
    </template>

    <UTable :columns="columns" :data="transactions" :loading="loading" class="flex-1 overflow-y-auto">
      <template #loading>
        <div class="flex flex-col items-center justify-center py-10 gap-3">
          <LoadingAnimation/>
          <p class="text-sm text-neutral-500">Loading transactions...</p>
        </div>
      </template>
      <template #empty>
        <div class="flex flex-col items-center justify-center py-10 gap-3">
          <UIcon class="w-8 h-8 text-neutral-400" name="i-lucide-database-x"/>
          <p class="text-sm text-neutral-500">No transactions found.</p>
        </div>
      </template>
    </UTable>
  </UCard>

  <div v-if="hasMore && transactions.length > 0" ref="loadMoreTrigger" class="flex justify-center p-4">
    <LoadingAnimation v-if="loadingMore || loading"/>
  </div>

  <CreateFab @create="onOpenCreateModal"/>

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
