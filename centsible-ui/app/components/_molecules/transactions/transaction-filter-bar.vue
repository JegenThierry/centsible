<script lang="ts" setup>
import {computed, ref, watch} from 'vue';
import type {TransactionFilters, TransactionSort} from "~/models/transactions/transaction-filters";
import TransactionCategoryFilter from "~/components/_molecules/transactions/transaction-category-filter.vue";
import TransactionDateRangeFilter from "~/components/_molecules/transactions/transaction-date-range-filter.vue";

const model = defineModel<TransactionFilters>({required: true});

const {t} = useI18n();

const sortOptions = computed(() => [
  {value: 'DATE_DESC', label: t('transactions.filters.sort.DATE_DESC')},
  {value: 'DATE_ASC', label: t('transactions.filters.sort.DATE_ASC')},
  {value: 'AMOUNT_DESC', label: t('transactions.filters.sort.AMOUNT_DESC')},
  {value: 'AMOUNT_ASC', label: t('transactions.filters.sort.AMOUNT_ASC')},
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

const hasFilters = computed(() =>
  !!searchDraft.value || categoryIds.value.length > 0 || !!fromDate.value || !!toDate.value || sort.value !== 'DATE_DESC',
);

function reset() {
  searchDraft.value = '';
  model.value = {sort: 'DATE_DESC'};
}
</script>

<template>
  <div class="flex flex-wrap items-center gap-2 mb-4">
    <UInput v-model="searchDraft"
            :placeholder="t('transactions.filters.searchPlaceholder')"
            class="flex-1 min-w-[200px] max-w-md"
            icon="i-lucide-search"
            size="sm"/>

    <TransactionCategoryFilter v-model="categoryIds"/>

    <TransactionDateRangeFilter v-model:from-date="fromDate" v-model:to-date="toDate"/>

    <USelect v-model="sort"
             :items="sortOptions"
             class="min-w-[160px]"
             icon="i-lucide-arrow-up-down"
             size="sm"/>

    <UButton v-if="hasFilters"
             :aria-label="t('transactions.filters.clear')"
             color="neutral"
             icon="i-lucide-x"
             size="sm"
             variant="ghost"
             @click="reset">
      {{ t('transactions.filters.clear') }}
    </UButton>
  </div>
</template>
