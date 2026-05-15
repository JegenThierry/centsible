<script lang="ts" setup>
import {useRecurringTransactionsStore} from "~/stores/recurringTransactionsStore";
import {useBudgetAccountsStore} from "~/stores/budgetAccountsStore";
import {useRecurringTransactionService} from "~/services/recurring/recurring-transaction-service";
import {useToasts} from "~/services/toasts/toast-service";
import type {RecurringTransaction} from "~/models/recurring/recurring-transaction";
import {Currency} from "~/models/budget-account/currency";
import CreateFab from "~/components/_molecules/buttons/create-fab.vue";
import RecurringRow from "~/components/_molecules/recurring/recurring-row.vue";
import CardSkeleton from "~/components/_molecules/skeletons/card-skeleton.vue";
import AppEmptyState from "~/components/_molecules/feedback/app-empty-state.vue";
import CreateRecurringModal from "~/components/_organisms/recurring/modals/create-recurring-modal.vue";
import EditRecurringModal from "~/components/_organisms/recurring/modals/edit-recurring-modal.vue";
import DeleteRecurringModal from "~/components/_organisms/recurring/modals/delete-recurring-modal.vue";

const store = useRecurringTransactionsStore();
const accountsStore = useBudgetAccountsStore();
const service = useRecurringTransactionService(useApi());
const toasts = useToasts();

const isCreateModalOpen = ref(false);
const isEditModalOpen = ref(false);
const isDeleteModalOpen = ref(false);
const selected = ref<RecurringTransaction | null>(null);

const currency = computed(() => accountsStore.activeAccount?.currency || Currency.EUR);

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
  try {
    if (rule.active) {
      await service.pause(rule.id);
      toasts.success('Rule paused.', 'No new occurrences will be generated.');
    } else {
      await service.resume(rule.id);
      toasts.success('Rule resumed.', 'Occurrences will be generated from the next due date.');
    }
    refresh();
  } catch (error) {
    toasts.error('Action failed.', 'Could not change the rule status.');
    console.error(error);
  }
}

watch(() => accountsStore.activeAccount?.id, () => refresh());

onMounted(() => refresh());
</script>

<template>
  <div>
    <div v-if="store.loading && store.items.length === 0" class="space-y-3">
      <CardSkeleton v-for="i in 3" :key="i"/>
    </div>

    <AppEmptyState v-else-if="store.items.length === 0"
                   description="Create a recurring rule to auto-generate transactions like rent, salary, or subscriptions."
                   icon="i-lucide-repeat"
                   title="No recurring rules yet">
      <template #actions>
        <UButton class="w-full sm:w-auto justify-center" @click="isCreateModalOpen = true">
          New recurring rule
        </UButton>
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
