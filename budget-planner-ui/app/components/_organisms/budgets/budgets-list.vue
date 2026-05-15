<script lang="ts" setup>
import {useBudgetsStore} from "~/stores/budgetsStore";
import {useBudgetAccountsStore} from "~/stores/budgetAccountsStore";
import type {Budget} from "~/models/budget/budget";
import {Currency} from "~/models/budget-account/currency";
import CreateFab from "~/components/_molecules/buttons/create-fab.vue";
import BudgetProgressBar from "~/components/_molecules/budgets/budget-progress-bar.vue";
import CardSkeleton from "~/components/_molecules/skeletons/card-skeleton.vue";
import AppEmptyState from "~/components/_molecules/feedback/app-empty-state.vue";
import CreateBudgetModal from "~/components/_organisms/budgets/modals/create-budget-modal.vue";
import EditBudgetModal from "~/components/_organisms/budgets/modals/edit-budget-modal.vue";
import DeleteBudgetModal from "~/components/_organisms/budgets/modals/delete-budget-modal.vue";

const store = useBudgetsStore();
const accountsStore = useBudgetAccountsStore();

const isCreateModalOpen = ref(false);
const isEditModalOpen = ref(false);
const isDeleteModalOpen = ref(false);
const selected = ref<Budget | null>(null);

const currency = computed(() => accountsStore.activeAccount?.currency || Currency.EUR);

function openEdit(b: Budget) {
  selected.value = b;
  isEditModalOpen.value = true;
}

function openDelete(b: Budget) {
  selected.value = b;
  isDeleteModalOpen.value = true;
}

function refresh() {
  store.fetchCurrentMonth();
}

onMounted(() => refresh());
</script>

<template>
  <div>
    <div v-if="store.loading && store.items.length === 0" class="space-y-3">
      <CardSkeleton v-for="i in 3" :key="i"/>
    </div>

    <AppEmptyState v-else-if="store.items.length === 0"
                   description="Set a monthly limit on a category to track your spending against it."
                   icon="i-lucide-target"
                   title="No budgets yet">
      <template #actions>
        <UButton class="w-full sm:w-auto justify-center" @click="isCreateModalOpen = true">
          New budget
        </UButton>
      </template>
    </AppEmptyState>

    <div v-else class="space-y-4">
      <UCard v-for="budget in store.items"
             :key="budget.id"
             :ui="{body: 'p-4 sm:p-5'}"
             variant="outline">
        <div class="flex items-start gap-4">
          <div class="flex-1 min-w-0">
            <BudgetProgressBar :budget="budget" :currency="currency"/>
          </div>
          <div class="flex items-center gap-1 shrink-0">
            <UButton aria-label="Edit budget"
                     color="neutral"
                     icon="i-lucide-pencil"
                     variant="ghost"
                     @click="openEdit(budget)"/>
            <UButton aria-label="Delete budget"
                     color="error"
                     icon="i-lucide-trash"
                     variant="ghost"
                     @click="openDelete(budget)"/>
          </div>
        </div>
      </UCard>
    </div>

    <CreateFab @create="isCreateModalOpen = true"/>

    <CreateBudgetModal v-if="isCreateModalOpen"
                       v-model:open="isCreateModalOpen"
                       @created="refresh"/>

    <EditBudgetModal v-if="isEditModalOpen && selected"
                     v-model:open="isEditModalOpen"
                     :budget="selected"
                     @updated="refresh"/>

    <DeleteBudgetModal v-if="isDeleteModalOpen"
                       v-model:open="isDeleteModalOpen"
                       :budget="selected"
                       @deleted="refresh"/>
  </div>
</template>
