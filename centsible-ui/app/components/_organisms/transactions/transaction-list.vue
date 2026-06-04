<script lang="ts" setup>
import {computed, ref} from 'vue'
import {useIntersectionObserver} from '@vueuse/core'
import {useBudgetAccountsStore} from "~/stores/budgetAccountsStore";
import {useTransactionService} from "~/services/transactions/transaction-service";
import {type Transaction} from "~/models/transactions/transaction";
import type {TransactionFilters} from "~/models/transactions/transaction-filters";
import {useActiveCurrency} from "~/composables/use-active-currency";
import {useToasts} from "~/services/toasts/toast-service";
import CreateFab from "~/components/_molecules/buttons/create-fab.vue";
import {useTransactionList} from "~/components/_organisms/transactions/utils/use-transaction-list";
import {createTransactionColumns} from "~/components/_organisms/transactions/utils/transaction-columns";
import LoadingAnimation from "~/components/_atoms/animations/loading-animation.vue";
import BaseTable from "~/components/_molecules/tables/base-table.vue";
import TransactionFilterBar from "~/components/_molecules/transactions/transaction-filter-bar.vue";
import TransactionBulkActionBar from "~/components/_molecules/transactions/transaction-bulk-action-bar.vue";

const EditTransactionModal = defineAsyncComponent(() => import("~/components/_organisms/transactions/modals/edit-transaction-modal.vue"));
const DeleteTransactionModal = defineAsyncComponent(() => import("~/components/_organisms/transactions/modals/delete-transaction-modal.vue"));
const BulkCategorizeModal = defineAsyncComponent(() => import("~/components/_organisms/transactions/modals/bulk-categorize-modal.vue"));
const CreateTransactionModal = defineAsyncComponent(() => import("~/components/_organisms/transactions/modals/create-transaction-modal.vue"));
const RuleModal = defineAsyncComponent(() => import("~/components/_organisms/categories/modals/rule-modal.vue"));

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

const isRuleModalOpen = ref(false);
const rulePresetPattern = ref('');
const rulePresetCategoryId = ref<number | undefined>(undefined);

function openCreateRule(transaction: Transaction) {
  rulePresetPattern.value = transaction.description;
  rulePresetCategoryId.value = transaction.category?.id;
  isRuleModalOpen.value = true;
}

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

// Columns are built once. Header strings use thunks so locale changes still surface, and cell
// closures read reactive state (currency, selection) lazily — TanStack calls them per render.
const columns = createTransactionColumns({
  t,
  currency: () => currency.value,
  isSelected: (id) => selectedIds.value.has(id),
  isAllSelected: () => allOnPageSelected.value,
  onToggleAll: toggleAll,
  onToggleOne: toggleOne,
  onEdit: openEditModal,
  onDelete: openDeleteModal,
  onCreateRule: openCreateRule,
})

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

  <TransactionBulkActionBar v-if="selectedIds.size > 0"
                            :count="selectedIds.size"
                            @recategorize="isBulkCategorizeOpen = true"
                            @delete="bulkDelete"
                            @clear="clearSelection"/>

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

  <RuleModal v-if="isRuleModalOpen"
             v-model:open="isRuleModalOpen"
             :preset-pattern="rulePresetPattern"
             :preset-category-id="rulePresetCategoryId"/>
</template>
