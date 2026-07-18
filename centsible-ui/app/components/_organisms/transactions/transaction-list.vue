<script lang="ts" setup>
import {computed, ref} from 'vue'
import {useBudgetAccountsStore} from "~/stores/budgetAccountsStore";
import {useCategoriesStore} from "~/stores/categoriesStore";
import {useTransactionService} from "~/services/transactions/transaction-service";
import {type Transaction} from "~/models/transactions/transaction";
import type {Category} from "~/models/category/category";
import type {TransactionFilters} from "~/models/transactions/transaction-filters";
import type {RecurringTransactionForm} from "~/models/recurring/recurring-transaction";
import {Frequency} from "~/models/recurring/recurring-transaction";
import {Currency} from "~/models/budget-account/currency";
import {transactionType} from "~/utils/transaction";
import {todayIsoDate} from "~/utils/date";
import {useActiveCurrency} from "~/composables/use-active-currency";
import {useToasts} from "~/services/toasts/toast-service";
import CreateFab from "~/components/_molecules/buttons/create-fab.vue";
import {useTransactionList} from "~/components/_organisms/transactions/utils/use-transaction-list";
import {createTransactionColumns} from "~/components/_organisms/transactions/utils/transaction-columns";
import LoadingAnimation from "~/components/_atoms/animations/loading-animation.vue";
import BaseTable from "~/components/_molecules/tables/base-table.vue";
import TransactionFilterBar from "~/components/_molecules/transactions/transaction-filter-bar.vue";
import TransactionBulkActionBar from "~/components/_molecules/transactions/transaction-bulk-action-bar.vue";
import SavedFilterBar from "~/components/_organisms/transactions/saved-filter-bar.vue";

const EditTransactionModal = defineAsyncComponent(() => import("~/components/_organisms/transactions/modals/edit-transaction-modal.vue"));
const EditTransferModal = defineAsyncComponent(() => import("~/components/_organisms/transactions/modals/edit-transfer-modal.vue"));
const DeleteTransactionModal = defineAsyncComponent(() => import("~/components/_organisms/transactions/modals/delete-transaction-modal.vue"));
const BulkCategorizeModal = defineAsyncComponent(() => import("~/components/_organisms/transactions/modals/bulk-categorize-modal.vue"));
const BulkTagsModal = defineAsyncComponent(() => import("~/components/_organisms/transactions/modals/bulk-tags-modal.vue"));
const CreateTransactionModal = defineAsyncComponent(() => import("~/components/_organisms/transactions/modals/create-transaction-modal.vue"));
const SplitIntoIousModal = defineAsyncComponent(() => import("~/components/_organisms/transactions/modals/split-into-ious-modal.vue"));
const RecurringModal = defineAsyncComponent(() => import("~/components/_organisms/recurring/modals/recurring-modal.vue"));
const RuleModal = defineAsyncComponent(() => import("~/components/_organisms/rules/modals/rule-modal.vue"));
const ConfirmationModal = defineAsyncComponent(() => import("~/components/_organisms/modals/confirmation-modal.vue"));

const api = useApi();
const transactionService = useTransactionService(api);
const budgetAccountsStore = useBudgetAccountsStore();
const categoriesStore = useCategoriesStore();
const currency = useActiveCurrency();
const toasts = useToasts();
const {t} = useI18n();

onMounted(() => {
  if (categoriesStore.categories.length === 0) categoriesStore.updateCategories();
});

const filters = ref<TransactionFilters>({sort: 'DATE_DESC'});

/** Sort alone never hides rows, so it doesn't count as an active filter. */
const hasActiveFilters = computed(() => {
  const f = filters.value;
  return !!(f.search || f.categoryIds?.length || f.tagIds?.length || f.fromDate || f.toDate
    || f.type || f.amountMin != null || f.amountMax != null);
});

function clearFilters() {
  filters.value = {sort: filters.value.sort ?? 'DATE_DESC'};
}

function applySavedFilter(saved: TransactionFilters) {
  filters.value = {...saved};
}

const {
  transactions,
  loading,
  loadingMore,
  hasMore,
  error,
  loadTransactions
} = useTransactionList(transactionService, budgetAccountsStore, 25, filters);

const UNDO_WINDOW_MS = 6000;

interface PendingDelete {
  accountId: string;
  timer: ReturnType<typeof setTimeout>;
  committing?: boolean;
}

const pendingDeletes = ref<Map<string, PendingDelete>>(new Map());

const visibleTransactions = computed(() =>
  pendingDeletes.value.size === 0
    ? transactions.value
    : transactions.value.filter((tx) => !pendingDeletes.value.has(tx.id)),
);

const isCreateModalOpen = ref(false);
const isEditModalOpen = ref(false);
const isEditTransferModalOpen = ref(false);
const isDeleteModalOpen = ref(false);
const isBulkDeleteOpen = ref(false);
const isBulkCategorizeOpen = ref(false);
const isBulkTagsOpen = ref(false);
const bulkTagsMode = ref<'add' | 'remove'>('add');
const isSplitModalOpen = ref(false);
const isRecurringModalOpen = ref(false);
const recurringSeed = ref<RecurringTransactionForm>();
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
  if (transaction.transferGroupId) {
    isEditTransferModalOpen.value = true;
  } else {
    isEditModalOpen.value = true;
  }
}

function openDeleteModal(transaction: Transaction) {
  selectedTransaction.value = transaction;
  isDeleteModalOpen.value = true;
}

function openSplitModal(transaction: Transaction) {
  selectedTransaction.value = transaction;
  isSplitModalOpen.value = true;
}

/** Seeds a new recurring rule from a transaction (amount/category/description/type), then opens the modal. */
function openMakeRecurring(transaction: Transaction) {
  const account = budgetAccountsStore.activeAccount;
  recurringSeed.value = {
    amount: Math.abs(transaction.amount),
    description: transaction.description,
    category: transaction.category,
    frequency: Frequency.MONTHLY,
    startDate: todayIsoDate(),
    endDate: undefined,
    active: true,
    currency: account?.currency ?? Currency.EUR,
    type: transactionType(transaction),
    isTransfer: false,
    sourceAccountId: account?.id,
    destinationAccountId: undefined,
  };
  isRecurringModalOpen.value = true;
}

async function onMutated() {
  await budgetAccountsStore.updateActiveAccount();
  await loadTransactions(true);
}

const allOnPageSelected = computed(() =>
  visibleTransactions.value.length > 0 && visibleTransactions.value.every((t) => selectedIds.value.has(t.id))
);

function toggleAll(checked: boolean) {
  const next = new Set(selectedIds.value);
  if (checked) visibleTransactions.value.forEach((t) => next.add(t.id));
  else visibleTransactions.value.forEach((t) => next.delete(t.id));
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

const columns = createTransactionColumns({
  t,
  currency: () => currency.value,
  categories: () => categoriesStore.categories,
  isSelected: (id) => selectedIds.value.has(id),
  isAllSelected: () => allOnPageSelected.value,
  onToggleAll: toggleAll,
  onToggleOne: toggleOne,
  onEdit: openEditModal,
  onDelete: openDeleteModal,
  onCreateRule: openCreateRule,
  onSplitIntoIous: openSplitModal,
  onMakeRecurring: openMakeRecurring,
  onCategorize,
})

async function bulkDelete() {
  const ids = Array.from(selectedIds.value);
  if (ids.length === 0 || !budgetAccountsStore.activeAccount) return;
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

function openBulkTags(mode: 'add' | 'remove') {
  bulkTagsMode.value = mode;
  isBulkTagsOpen.value = true;
}

async function applyBulkTags(mode: 'add' | 'remove', tagIds: number[]) {
  const ids = Array.from(selectedIds.value);
  if (ids.length === 0 || tagIds.length === 0 || !budgetAccountsStore.activeAccount) return;
  const accountId = budgetAccountsStore.activeAccount.id;
  try {
    if (mode === 'add') await transactionService.bulkAddTags(accountId, ids, tagIds);
    else await transactionService.bulkRemoveTags(accountId, ids, tagIds);
    toasts.success(t(`transactions.bulk.${mode}TagsToastTitle`), t(`transactions.bulk.${mode}TagsToastBody`, {count: ids.length}));
    clearSelection();
    await loadTransactions(true);
  } catch (e) {
    toasts.error(t('transactions.bulk.errorTitle'), t('transactions.bulk.errorBody'));
  }
}

function patchCategory(id: string, category: Category) {
  transactions.value = transactions.value.map((tx) => (tx.id === id ? {...tx, category} : tx));
}

/** Inline single-row categorize: applies optimistically, reuses bulk-categorize, rolls back on error. */
async function onCategorize(transaction: Transaction, category: Category) {
  const account = budgetAccountsStore.activeAccount;
  if (!account || transaction.category?.id === category.id) return;
  const previousCategory = transaction.category;
  patchCategory(transaction.id, category);
  try {
    await transactionService.bulkCategorize(account.id, [transaction.id], category.id);
    toasts.success(
      t('transactions.category.updateToastTitle'),
      t('transactions.category.updateToastBody', {category: category.name}),
    );
  } catch (e) {
    patchCategory(transaction.id, previousCategory);
    toasts.error(t('transactions.bulk.errorTitle'), t('transactions.bulk.errorBody'));
  }
}

/** Removes an id from the pending set, which un-hides its row in place. */
function releasePending(id: string) {
  if (!pendingDeletes.value.has(id)) return;
  const next = new Map(pendingDeletes.value);
  next.delete(id);
  pendingDeletes.value = next;
}

/** Fires the real DELETE once the undo window lapses; un-hides the row if the server refuses. */
async function commitDelete(id: string) {
  const pending = pendingDeletes.value.get(id);
  if (!pending || pending.committing) return;
  pending.committing = true;
  clearTimeout(pending.timer);
  try {
    await transactionService.deleteTransaction(pending.accountId, id);
    transactions.value = transactions.value.filter((tx) => tx.id !== id);
    releasePending(id);
    if (budgetAccountsStore.activeAccount?.id === pending.accountId) {
      await budgetAccountsStore.updateActiveAccount();
    }
  } catch (e) {
    releasePending(id);
    const entity = t('transactions.delete.entity');
    toasts.error(t('common.confirmDelete.errorTitle', {entity}), t('common.confirmDelete.errorBody', {entity}));
  }
}

/** User hit Undo inside the window: cancel the pending delete so the row reappears. */
function undoDelete(id: string) {
  const pending = pendingDeletes.value.get(id);
  if (!pending || pending.committing) return;
  clearTimeout(pending.timer);
  releasePending(id);
}

/** Commits every still-pending delete immediately — used before unmount / account switch. */
function flushPendingDeletes() {
  for (const id of Array.from(pendingDeletes.value.keys())) void commitDelete(id);
}

/**
 * Browser teardown, which `onBeforeUnmount` never sees: closing the tab, F5, or following an
 * external link inside the undo window would otherwise drop the DELETE entirely — the user watched
 * the row vanish and got a "deleted" toast, but the transaction is still there next session. In a
 * finance app that reads as corruption, so the pending deletes go out as keepalive requests that
 * outlive the document.
 *
 * Marked `committing` so a bfcache restore doesn't let the timers fire a second DELETE for rows
 * that have already been sent.
 */
function flushPendingDeletesOnUnload() {
  for (const [id, pending] of pendingDeletes.value) {
    if (pending.committing) continue;
    pending.committing = true;
    clearTimeout(pending.timer);
    transactionService.deleteTransactionOnUnload(pending.accountId, id);
  }
}

useEventListener('pagehide', flushPendingDeletesOnUnload);

/** Transfers span two ledger rows and can't be cleanly restored — delete straight away, no undo. */
async function deleteTransferNow(accountId: string, transaction: Transaction) {
  const entity = t('transactions.transfer.entity');
  try {
    await transactionService.deleteTransaction(accountId, transaction.id);
    toasts.success(t('common.confirmDelete.successTitle', {entity}), t('common.confirmDelete.successBody', {entity}));
    await onMutated();
  } catch (e) {
    toasts.error(t('common.confirmDelete.errorTitle', {entity}), t('common.confirmDelete.errorBody', {entity}));
  }
}

/** Confirmed delete: transfers go immediately, regular rows hide behind an undo window. */
function onDeleteConfirmed() {
  const transaction = selectedTransaction.value;
  const account = budgetAccountsStore.activeAccount;
  if (!transaction || !account) return;

  if (transaction.transferGroupId) {
    void deleteTransferNow(account.id, transaction);
    return;
  }

  const timer = setTimeout(() => void commitDelete(transaction.id), UNDO_WINDOW_MS);
  const next = new Map(pendingDeletes.value);
  next.set(transaction.id, {accountId: account.id, timer});
  pendingDeletes.value = next;

  toasts.action(
    'info',
    t('transactions.delete.undoTitle'),
    t('transactions.delete.undoBody'),
    t('common.actions.undo'),
    () => undoDelete(transaction.id),
    UNDO_WINDOW_MS,
  );
}

onBeforeUnmount(flushPendingDeletes);

watch(
  () => budgetAccountsStore.activeAccount?.id,
  (id) => {
    if (id) {
      flushPendingDeletes();
      clearSelection();
      loadTransactions(true);
    }
  },
  {immediate: true},
)
</script>

<template>
  <TransactionFilterBar v-model="filters"/>

  <SavedFilterBar :can-save="hasActiveFilters" :filters="filters" @apply="applySavedFilter"/>

  <TransactionBulkActionBar v-if="selectedIds.size > 0"
                            :count="selectedIds.size"
                            @recategorize="isBulkCategorizeOpen = true"
                            @add-tags="openBulkTags('add')"
                            @remove-tags="openBulkTags('remove')"
                            @delete="isBulkDeleteOpen = true"
                            @clear="clearSelection"/>

  <BaseTable :columns="columns"
             :data="visibleTransactions"
             :loading="loading"
             :error="error"
             :empty-title="t('transactions.emptyTitle')"
             :filtered="hasActiveFilters"
             :filtered-title="t('transactions.filters.noResults')"
             :loading-message="t('transactions.loadingMessage')"
             virtualize
             :can-load-more="hasMore && !loading && !loadingMore"
             @load-more="loadTransactions()"
             @clear-filters="clearFilters"
             @retry="loadTransactions(true)"/>

  <div v-if="loadingMore" class="flex justify-center p-4">
    <LoadingAnimation/>
  </div>

  <CreateFab @create="isCreateModalOpen = true"/>

  <CreateTransactionModal v-if="isCreateModalOpen"
                          v-model:open="isCreateModalOpen"
                          @created="onMutated"/>

  <EditTransactionModal v-if="isEditModalOpen && selectedTransaction !== null"
                        v-model:open="isEditModalOpen"
                        :transaction="selectedTransaction"
                        @updated="onMutated"/>

  <EditTransferModal v-if="isEditTransferModalOpen && selectedTransaction !== null"
                     v-model:open="isEditTransferModalOpen"
                     :transaction="selectedTransaction"
                     @updated="onMutated"/>

  <SplitIntoIousModal v-if="isSplitModalOpen && selectedTransaction !== null"
                      v-model:open="isSplitModalOpen"
                      :transaction="selectedTransaction"
                      :currency="currency"/>

  <RecurringModal v-if="isRecurringModalOpen"
                  v-model:open="isRecurringModalOpen"
                  :seed="recurringSeed"/>

  <DeleteTransactionModal v-if="isDeleteModalOpen"
                          v-model:open="isDeleteModalOpen"
                          :transaction="selectedTransaction"
                          @confirm="onDeleteConfirmed"/>

  <BulkCategorizeModal v-if="isBulkCategorizeOpen"
                       v-model:open="isBulkCategorizeOpen"
                       :count="selectedIds.size"
                       @confirm="bulkCategorize"/>

  <BulkTagsModal v-if="isBulkTagsOpen"
                 v-model:open="isBulkTagsOpen"
                 :count="selectedIds.size"
                 :mode="bulkTagsMode"
                 @confirm="(tagIds) => applyBulkTags(bulkTagsMode, tagIds)"/>

  <ConfirmationModal v-if="isBulkDeleteOpen"
                     v-model:open="isBulkDeleteOpen"
                     :title="t('common.confirmDelete.title')"
                     :body="t('transactions.bulk.deleteConfirm', {count: selectedIds.size})"
                     :confirm-label="t('transactions.bulk.delete')"
                     :delete-callback="bulkDelete"
                     :manage-toasts="false"/>

  <RuleModal v-if="isRuleModalOpen"
             v-model:open="isRuleModalOpen"
             :preset-pattern="rulePresetPattern"
             :preset-category-id="rulePresetCategoryId"/>
</template>
