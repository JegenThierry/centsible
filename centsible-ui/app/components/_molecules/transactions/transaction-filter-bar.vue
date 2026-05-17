<script lang="ts" setup>
import {computed, ref, watch} from 'vue';
import type {TransactionFilters, TransactionSort} from "~/models/transactions/transaction-filters";
import TransactionCategoryFilter from "~/components/_molecules/transactions/transaction-category-filter.vue";
import TransactionDateRangeFilter from "~/components/_molecules/transactions/transaction-date-range-filter.vue";

const props = defineProps<{
  modelValue: TransactionFilters;
}>();

const emit = defineEmits<{
  (e: 'update:modelValue', value: TransactionFilters): void;
}>();

const {t} = useI18n();

const search = ref(props.modelValue.search ?? '');
const categoryIds = ref<number[]>(props.modelValue.categoryIds ?? []);
const fromDate = ref(props.modelValue.fromDate ?? '');
const toDate = ref(props.modelValue.toDate ?? '');
const sort = ref<TransactionSort>(props.modelValue.sort ?? 'DATE_DESC');

const sortOptions = computed(() => [
  {value: 'DATE_DESC', label: t('transactions.filters.sort.DATE_DESC')},
  {value: 'DATE_ASC', label: t('transactions.filters.sort.DATE_ASC')},
  {value: 'AMOUNT_DESC', label: t('transactions.filters.sort.AMOUNT_DESC')},
  {value: 'AMOUNT_ASC', label: t('transactions.filters.sort.AMOUNT_ASC')},
]);

const hasFilters = computed(() =>
  !!search.value || categoryIds.value.length > 0 || !!fromDate.value || !!toDate.value || sort.value !== 'DATE_DESC',
);

let debounce: ReturnType<typeof setTimeout> | null = null;
watch([search, categoryIds, fromDate, toDate, sort], () => {
  if (debounce) clearTimeout(debounce);
  debounce = setTimeout(() => {
    emit('update:modelValue', {
      search: search.value || undefined,
      categoryIds: categoryIds.value.length ? categoryIds.value : undefined,
      fromDate: fromDate.value || undefined,
      toDate: toDate.value || undefined,
      sort: sort.value,
    });
  }, 250);
}, {deep: true});

function reset() {
  search.value = '';
  categoryIds.value = [];
  fromDate.value = '';
  toDate.value = '';
  sort.value = 'DATE_DESC';
}
</script>

<template>
  <div class="flex flex-wrap items-center gap-2 mb-4">
    <UInput v-model="search"
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
