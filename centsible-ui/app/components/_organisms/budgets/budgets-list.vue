<script lang="ts" setup>
import {useBudgetsStore} from "~/stores/budgetsStore";
import type {Budget} from "~/models/budget/budget";
import CreateFab from "~/components/_molecules/buttons/create-fab.vue";
import BudgetProgressBar from "~/components/_molecules/budgets/budget-progress-bar.vue";
import CardSkeleton from "~/components/_molecules/skeletons/card-skeleton.vue";
import AppEmptyState from "~/components/_molecules/feedback/app-empty-state.vue";
const CreateBudgetModal = defineAsyncComponent(() => import("~/components/_organisms/budgets/modals/create-budget-modal.vue"));
const EditBudgetModal = defineAsyncComponent(() => import("~/components/_organisms/budgets/modals/edit-budget-modal.vue"));
const DeleteBudgetModal = defineAsyncComponent(() => import("~/components/_organisms/budgets/modals/delete-budget-modal.vue"));
import {useActiveCurrency} from "~/composables/use-active-currency";
import {format, parseISO, subMonths} from 'date-fns';

const MONTH_FMT = 'yyyy-MM';

const store = useBudgetsStore();
const {t} = useI18n();

const isCreateModalOpen = ref(false);
const isEditModalOpen = ref(false);
const isDeleteModalOpen = ref(false);
const selected = ref<Budget | null>(null);

const currency = useActiveCurrency();

function monthsBack(from: string, count: number): string {
  return format(subMonths(parseISO(`${from}-01`), count), MONTH_FMT);
}

const currentMonth = format(new Date(), MONTH_FMT);
const selectedMonth = ref<string>(currentMonth);
const showHistory = ref(false);

const historyMonths = computed(() =>
  Array.from({length: 6}, (_, i) => monthsBack(currentMonth, i)),
);

const monthItems = computed(() =>
  Array.from({length: 24}, (_, i) => {
    const m = monthsBack(currentMonth, i);
    return {label: m, value: m};
  }),
);

function openEdit(b: Budget) {
  selected.value = b;
  isEditModalOpen.value = true;
}

function openDelete(b: Budget) {
  selected.value = b;
  isDeleteModalOpen.value = true;
}

async function refresh() {
  await store.fetchForMonth(selectedMonth.value);
  if (showHistory.value) await store.fetchHistory(historyMonths.value);
}

watch(selectedMonth, () => refresh());
watch(showHistory, (open) => {
  if (open) store.fetchHistory(historyMonths.value);
});

onMounted(() => refresh());
</script>

<template>
  <div class="space-y-4">
    <div class="flex flex-wrap items-end gap-3">
      <div class="flex flex-col gap-1">
        <label class="text-xs font-semibold text-muted uppercase tracking-wide">
          {{ t('budgets.list.asOf') }}
        </label>
        <USelect v-model="selectedMonth"
                 :items="monthItems"
                 class="w-40"
                 value-key="value"/>
      </div>
      <UButton :color="showHistory ? 'primary' : 'neutral'"
               :variant="showHistory ? 'soft' : 'ghost'"
               icon="i-lucide-history"
               size="sm"
               @click="showHistory = !showHistory">
        {{ t('budgets.list.showHistory') }}
      </UButton>
    </div>

    <div v-if="store.loading && store.items.length === 0" class="space-y-3">
      <CardSkeleton v-for="i in 3" :key="i"/>
    </div>

    <AppEmptyState v-else-if="store.items.length === 0 && !showHistory"
                   :description="t('budgets.list.emptyDescription')"
                   icon="i-lucide-target"
                   :title="t('budgets.list.emptyTitle')">
      <template #actions>
        <UButton class="w-full sm:w-auto justify-center" @click="isCreateModalOpen = true">
          {{ t('budgets.list.emptyAction') }}
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
          <div v-if="selectedMonth === currentMonth" class="flex items-center gap-1 shrink-0">
            <UButton :aria-label="t('budgets.list.editAria')"
                     color="neutral"
                     icon="i-lucide-pencil"
                     variant="ghost"
                     @click="openEdit(budget)"/>
            <UButton :aria-label="t('budgets.list.deleteAria')"
                     color="error"
                     icon="i-lucide-trash"
                     variant="ghost"
                     @click="openDelete(budget)"/>
          </div>
        </div>
      </UCard>
    </div>

    <section v-if="showHistory" class="space-y-4 pt-4 border-t border-default">
      <h2 class="text-base font-semibold text-highlighted">{{ t('budgets.list.historyHeading') }}</h2>

      <div v-for="period in store.history" :key="period.month" class="space-y-2">
        <h3 class="text-sm font-medium text-muted">{{ period.month }}</h3>
        <div v-if="period.budgets.length === 0" class="text-xs text-muted">
          {{ t('budgets.list.noBudgetsInPeriod') }}
        </div>
        <UCard v-for="b in period.budgets"
               :key="b.id"
               :ui="{body: 'p-3 sm:p-4'}"
               variant="outline">
          <BudgetProgressBar :budget="b" :currency="currency"/>
        </UCard>
      </div>
    </section>

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
