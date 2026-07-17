<script lang="ts" setup>
import {useBudgetsStore} from "~/stores/budgetsStore";
import type {Budget} from "~/models/budget/budget";
import CreateFab from "~/components/_molecules/buttons/create-fab.vue";
import BudgetProgressBar from "~/components/_molecules/budgets/budget-progress-bar.vue";
import BudgetSummary from "~/components/_molecules/budgets/budget-summary.vue";
import CardSkeleton from "~/components/_molecules/skeletons/card-skeleton.vue";
import AppEmptyState from "~/components/_molecules/feedback/app-empty-state.vue";
import EditDeleteActions from "~/components/_molecules/buttons/edit-delete-actions.vue";
import AppButton from "~/components/_atoms/ui/app-button.vue";
import AppSelect from "~/components/_atoms/ui/app-select.vue";
const BudgetModal = defineAsyncComponent(() => import("~/components/_organisms/budgets/modals/budget-modal.vue"));
const DeleteBudgetModal = defineAsyncComponent(() => import("~/components/_organisms/budgets/modals/delete-budget-modal.vue"));
import {useDefaultCurrency} from "~/composables/use-default-currency";
import {format, parseISO, subMonths} from 'date-fns';
import {formatMonthYearLabel} from "~/utils/date";

const MONTH_FMT = 'yyyy-MM';

const store = useBudgetsStore();
const {t} = useI18n();
const localeTag = useLocaleTag();

function monthLabel(m: string): string {
  return formatMonthYearLabel(m, localeTag.value);
}

const isCreateModalOpen = ref(false);
const isEditModalOpen = ref(false);
const isDeleteModalOpen = ref(false);
const selected = ref<Budget | null>(null);

const existingCombos = computed(() =>
  store.items.map((b) => ({categoryId: b.category.id, periodType: b.periodType})),
);

const currency = useDefaultCurrency();

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
    return {label: monthLabel(m), value: m};
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
  await Promise.all([
    store.fetchForMonth(selectedMonth.value),
    showHistory.value ? store.fetchHistory(historyMonths.value) : Promise.resolve(),
  ]);
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
        <AppSelect v-model="selectedMonth"
                 :items="monthItems"
                 class="w-40"
                 value-key="value"/>
      </div>
      <AppButton :color="showHistory ? 'primary' : 'neutral'"
               :variant="showHistory ? 'soft' : 'ghost'"
               icon="i-lucide-history"
               size="sm"
               @click="showHistory = !showHistory">
        {{ t('budgets.list.showHistory') }}
      </AppButton>
    </div>

    <div v-if="store.loading && store.items.length === 0" class="space-y-3">
      <CardSkeleton v-for="i in 3" :key="i"/>
    </div>

    <AppEmptyState v-else-if="store.error && store.items.length === 0"
                   icon="i-lucide-triangle-alert"
                   :title="t('common.states.error')">
      <template #actions>
        <AppButton class="w-full sm:w-auto justify-center"
                   color="neutral"
                   variant="soft"
                   icon="i-lucide-refresh-cw"
                   @click="refresh">
          {{ t('common.actions.retry') }}
        </AppButton>
      </template>
    </AppEmptyState>

    <AppEmptyState v-else-if="store.items.length === 0 && !showHistory"
                   :description="t('budgets.list.emptyDescription')"
                   icon="i-lucide-target"
                   :title="t('budgets.list.emptyTitle')">
      <template #actions>
        <AppButton class="w-full sm:w-auto justify-center" @click="isCreateModalOpen = true">
          {{ t('budgets.list.emptyAction') }}
        </AppButton>
      </template>
    </AppEmptyState>

    <div v-else class="space-y-4">
      <BudgetSummary :budgets="store.items" :currency="currency" :month="selectedMonth"/>

      <UCard v-for="budget in store.items"
             :key="budget.id"
             :ui="{body: 'p-4 sm:p-5'}"
             variant="outline">
        <div class="flex items-start gap-4">
          <div class="flex-1 min-w-0">
            <BudgetProgressBar :budget="budget" :currency="currency"/>
          </div>
          <EditDeleteActions v-if="selectedMonth === currentMonth"
                             class="shrink-0"
                             :delete-aria-label="t('budgets.list.deleteAria')"
                             :edit-aria-label="t('budgets.list.editAria')"
                             @edit="openEdit(budget)"
                             @delete="openDelete(budget)"/>
        </div>
      </UCard>
    </div>

    <section v-if="showHistory" class="space-y-4 pt-4 border-t border-default">
      <h2 class="text-base font-semibold text-highlighted">{{ t('budgets.list.historyHeading') }}</h2>

      <AppEmptyState v-if="store.historyError"
                     icon="i-lucide-triangle-alert"
                     :title="t('common.states.error')">
        <template #actions>
          <AppButton class="w-full sm:w-auto justify-center"
                     color="neutral"
                     variant="soft"
                     icon="i-lucide-refresh-cw"
                     @click="store.fetchHistory(historyMonths)">
            {{ t('common.actions.retry') }}
          </AppButton>
        </template>
      </AppEmptyState>

      <div v-for="period in store.history" :key="period.month" class="space-y-2">
        <h3 class="text-sm font-medium text-muted capitalize">{{ monthLabel(period.month) }}</h3>
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

    <BudgetModal v-if="isCreateModalOpen"
                 v-model:open="isCreateModalOpen"
                 :existing-combos="existingCombos"
                 @created="refresh"/>

    <BudgetModal v-if="isEditModalOpen && selected"
                 v-model:open="isEditModalOpen"
                 :budget="selected"
                 @updated="refresh"/>

    <DeleteBudgetModal v-if="isDeleteModalOpen"
                       v-model:open="isDeleteModalOpen"
                       :budget="selected"
                       @deleted="refresh"/>
  </div>
</template>
