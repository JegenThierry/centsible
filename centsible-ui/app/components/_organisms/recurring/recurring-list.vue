<script lang="ts" setup>
import adze from 'adze'
import {useRecurringTransactionsStore} from "~/stores/recurringTransactionsStore";
import {useBudgetAccountsStore} from "~/stores/budgetAccountsStore";
import {useRecurringTransactionService} from "~/services/recurring/recurring-transaction-service";
import {useToasts} from "~/services/toasts/toast-service";
import type {RecurringTransaction} from "~/models/recurring/recurring-transaction";
import CreateFab from "~/components/_molecules/buttons/create-fab.vue";
import RecurringRow from "~/components/_molecules/recurring/recurring-row.vue";
import CardSkeleton from "~/components/_molecules/skeletons/card-skeleton.vue";
import AppEmptyState from "~/components/_molecules/feedback/app-empty-state.vue";
import AppButton from "~/components/_atoms/ui/app-button.vue";
const CreateRecurringModal = defineAsyncComponent(() => import("~/components/_organisms/recurring/modals/create-recurring-modal.vue"));
const EditRecurringModal = defineAsyncComponent(() => import("~/components/_organisms/recurring/modals/edit-recurring-modal.vue"));
const DeleteRecurringModal = defineAsyncComponent(() => import("~/components/_organisms/recurring/modals/delete-recurring-modal.vue"));
import {useActiveCurrency} from "~/composables/use-active-currency";

const store = useRecurringTransactionsStore();
const accountsStore = useBudgetAccountsStore();
const service = useRecurringTransactionService(useApi());
const toasts = useToasts();
const {t} = useI18n();

const isCreateModalOpen = ref(false);
const isEditModalOpen = ref(false);
const isDeleteModalOpen = ref(false);
const selected = ref<RecurringTransaction | null>(null);

const currency = useActiveCurrency();

function refresh() {
  const id = accountsStore.activeAccount?.id;
  if (id) store.fetchForAccount(id);
}

function openEdit(rule: RecurringTransaction) {
  selected.value = rule;
  isEditModalOpen.value = true;
}

function openDelete(rule: RecurringTransaction) {
  selected.value = rule;
  isDeleteModalOpen.value = true;
}

async function toggle(rule: RecurringTransaction) {
  const action = rule.active
    ? {op: service.pause, titleKey: 'transactions.recurring.toastPausedTitle', bodyKey: 'transactions.recurring.toastPausedBody'}
    : {op: service.resume, titleKey: 'transactions.recurring.toastResumedTitle', bodyKey: 'transactions.recurring.toastResumedBody'};
  try {
    await action.op(rule.id);
    toasts.success(t(action.titleKey), t(action.bodyKey));
    refresh();
  } catch (error) {
    toasts.error(t('transactions.recurring.toastToggleErrorTitle'), t('transactions.recurring.toastToggleErrorBody'));
    adze.ns('recurring').error('Toggle recurring rule failed', error);
  }
}

watch(() => accountsStore.activeAccount?.id, () => refresh(), {immediate: true});
</script>

<template>
  <div>
    <div v-if="store.loading && store.items.length === 0" class="space-y-3">
      <CardSkeleton v-for="i in 3" :key="i"/>
    </div>

    <AppEmptyState v-else-if="store.items.length === 0"
                   :description="t('transactions.recurring.emptyDescription')"
                   icon="i-lucide-repeat"
                   :title="t('transactions.recurring.emptyTitle')">
      <template #actions>
        <AppButton class="w-full sm:w-auto justify-center" @click="isCreateModalOpen = true">
          {{ t('transactions.recurring.newRule') }}
        </AppButton>
      </template>
    </AppEmptyState>

    <div v-else class="space-y-3">
      <RecurringRow v-for="rule in store.items"
                    :key="rule.id"
                    :currency="currency"
                    :rule="rule"
                    @delete="openDelete"
                    @edit="openEdit"
                    @toggle="toggle"/>
    </div>

    <CreateFab @create="isCreateModalOpen = true"/>

    <CreateRecurringModal v-if="isCreateModalOpen"
                          v-model:open="isCreateModalOpen"
                          @created="refresh"/>

    <EditRecurringModal v-if="isEditModalOpen && selected"
                        v-model:open="isEditModalOpen"
                        :rule="selected"
                        @updated="refresh"/>

    <DeleteRecurringModal v-if="isDeleteModalOpen"
                          v-model:open="isDeleteModalOpen"
                          :rule="selected"
                          @deleted="refresh"/>
  </div>
</template>
