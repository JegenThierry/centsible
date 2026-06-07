<script lang="ts" setup>
import {computed, ref, watch} from 'vue';
import type {TransactionFilters, TransactionSort, TransactionTypeFilter} from "~/models/transactions/transaction-filters";
import TransactionCategoryFilter from "~/components/_molecules/transactions/transaction-category-filter.vue";
import TransactionDateRangeFilter from "~/components/_molecules/transactions/transaction-date-range-filter.vue";
import AppInput from "~/components/_atoms/ui/app-input.vue";
import AppSelect from "~/components/_atoms/ui/app-select.vue";
import AppButton from "~/components/_atoms/ui/app-button.vue";

const model = defineModel<TransactionFilters>({required: true});

const {t} = useI18n();

const sortOptions = computed(() => [
  {value: 'DATE_DESC', label: t('transactions.filters.sort.DATE_DESC')},
  {value: 'DATE_ASC', label: t('transactions.filters.sort.DATE_ASC')},
  {value: 'AMOUNT_DESC', label: t('transactions.filters.sort.AMOUNT_DESC')},
  {value: 'AMOUNT_ASC', label: t('transactions.filters.sort.AMOUNT_ASC')},
]);

const typeOptions = computed(() => [
  {value: '', label: t('transactions.filters.type.all')},
  {value: 'INCOME', label: t('transactions.filters.type.income')},
  {value: 'EXPENSE', label: t('transactions.filters.type.expense')},
]);

function patch(partial: Partial<TransactionFilters>) {
  model.value = {...model.value, ...partial};
}

// Debounce search-text input only: every other control is a discrete pick that should fire instantly.
const searchDraft = ref(model.value.search ?? '');
watch(() => model.value.search, (v) => { if ((v ?? '') !== searchDraft.value) searchDraft.value = v ?? ''; });

const applySearch = useDebounceFn((v: string) => patch({search: v || undefined}), 250);
watch(searchDraft, (v) => { applySearch(v); });

const categoryIds = computed({
  get: () => model.value.categoryIds ?? [],
  set: (v) => patch({categoryIds: v.length ? v : undefined}),
});

const fromDate = computed({
  get: () => model.value.fromDate ?? '',
  set: (v) => patch({fromDate: v || undefined}),
});

const toDate = computed({
  get: () => model.value.toDate ?? '',
  set: (v) => patch({toDate: v || undefined}),
});

const sort = computed({
  get: () => model.value.sort ?? 'DATE_DESC',
  set: (v: TransactionSort) => patch({sort: v}),
});

const type = computed({
  get: () => model.value.type ?? '',
  set: (v: string) => patch({type: (v || undefined) as TransactionTypeFilter | undefined}),
});

// Amount bounds are free-typed numbers, so debounce them like search rather than reloading per keystroke.
const amountMinDraft = ref(model.value.amountMin != null ? String(model.value.amountMin) : '');
const amountMaxDraft = ref(model.value.amountMax != null ? String(model.value.amountMax) : '');
watch(() => model.value.amountMin, (v) => { const s = v != null ? String(v) : ''; if (s !== amountMinDraft.value) amountMinDraft.value = s; });
watch(() => model.value.amountMax, (v) => { const s = v != null ? String(v) : ''; if (s !== amountMaxDraft.value) amountMaxDraft.value = s; });
const applyAmount = (key: 'amountMin' | 'amountMax') => useDebounceFn((v: string) => {
  const n = parseFloat(v);
  patch({[key]: Number.isFinite(n) ? n : undefined});
}, 300);
const applyAmountMin = applyAmount('amountMin');
const applyAmountMax = applyAmount('amountMax');
watch(amountMinDraft, (v) => applyAmountMin(v));
watch(amountMaxDraft, (v) => applyAmountMax(v));

const hasFilters = computed(() =>
  !!searchDraft.value || categoryIds.value.length > 0 || !!fromDate.value || !!toDate.value
  || !!type.value || !!amountMinDraft.value || !!amountMaxDraft.value || sort.value !== 'DATE_DESC',
);

function reset() {
  searchDraft.value = '';
  amountMinDraft.value = '';
  amountMaxDraft.value = '';
  model.value = {sort: 'DATE_DESC'};
}
</script>

<template>
  <div class="flex flex-wrap items-center gap-2 mb-4">
    <AppInput v-model="searchDraft"
            :placeholder="t('transactions.filters.searchPlaceholder')"
            class="flex-1 min-w-[200px] max-w-md"
            icon="i-lucide-search"
            size="sm"/>

    <TransactionCategoryFilter v-model="categoryIds"/>

    <TransactionDateRangeFilter v-model:from-date="fromDate" v-model:to-date="toDate"/>

    <div class="flex items-center gap-1">
      <AppInput v-model="amountMinDraft"
              :placeholder="t('transactions.filters.amountMinPlaceholder')"
              class="w-24"
              inputmode="decimal"
              size="sm"
              type="number"/>
      <span class="text-muted text-sm">–</span>
      <AppInput v-model="amountMaxDraft"
              :placeholder="t('transactions.filters.amountMaxPlaceholder')"
              class="w-24"
              inputmode="decimal"
              size="sm"
              type="number"/>
    </div>

    <AppSelect v-model="type"
             :items="typeOptions"
             class="min-w-[140px]"
             icon="i-lucide-filter"
             size="sm"/>

    <AppSelect v-model="sort"
             :items="sortOptions"
             class="min-w-[160px]"
             icon="i-lucide-arrow-up-down"
             size="sm"/>

    <AppButton v-if="hasFilters"
             :aria-label="t('transactions.filters.clear')"
             color="neutral"
             icon="i-lucide-x"
             size="sm"
             variant="ghost"
             @click="reset">
      {{ t('transactions.filters.clear') }}
    </AppButton>
  </div>
</template>
