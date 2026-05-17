<script lang="ts" setup>
import {computed, ref, watch} from 'vue';
import type {TransactionFilters, TransactionSort} from "~/models/transactions/transaction-filters";
import {useCategoriesStore} from "~/stores/categoriesStore";

const props = defineProps<{
  modelValue: TransactionFilters;
}>();

const emit = defineEmits<{
  (e: 'update:modelValue', value: TransactionFilters): void;
}>();

const {t} = useI18n();
const categoriesStore = useCategoriesStore();

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

const categoryOptions = computed(() =>
  categoriesStore.categories.map((c) => ({value: c.id, label: c.name})),
);

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

onMounted(() => {
  if (categoriesStore.categories.length === 0) categoriesStore.updateCategories();
});
</script>

<template>
  <div class="flex flex-wrap gap-2 items-end mb-4">
    <div class="flex-1 min-w-[200px]">
      <label class="text-xs font-medium text-neutral-600 dark:text-neutral-300">{{ t('transactions.filters.searchLabel') }}</label>
      <UInput v-model="search"
              :placeholder="t('transactions.filters.searchPlaceholder')"
              class="w-full mt-1"
              icon="i-lucide-search"/>
    </div>

    <div class="min-w-[180px]">
      <label class="text-xs font-medium text-neutral-600 dark:text-neutral-300">{{ t('transactions.filters.categoryLabel') }}</label>
      <USelectMenu v-model="categoryIds"
                   :items="categoryOptions"
                   :placeholder="t('transactions.filters.categoryPlaceholder')"
                   multiple
                   value-key="value"
                   class="w-full mt-1"/>
    </div>

    <div>
      <label class="text-xs font-medium text-neutral-600 dark:text-neutral-300">{{ t('transactions.filters.fromLabel') }}</label>
      <UInput v-model="fromDate" class="mt-1" type="date"/>
    </div>

    <div>
      <label class="text-xs font-medium text-neutral-600 dark:text-neutral-300">{{ t('transactions.filters.toLabel') }}</label>
      <UInput v-model="toDate" class="mt-1" type="date"/>
    </div>

    <div class="min-w-[160px]">
      <label class="text-xs font-medium text-neutral-600 dark:text-neutral-300">{{ t('transactions.filters.sortLabel') }}</label>
      <USelect v-model="sort" :items="sortOptions" class="w-full mt-1"/>
    </div>

    <UButton v-if="hasFilters"
             color="neutral"
             icon="i-lucide-x"
             size="sm"
             variant="ghost"
             @click="reset">
      {{ t('transactions.filters.clear') }}
    </UButton>
  </div>
</template>
