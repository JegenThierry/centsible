<script lang="ts" setup generic="T">
import type {TableColumn} from '@nuxt/ui';
import {useInfiniteScroll} from '@vueuse/core';
import LoadingAnimation from "~/components/_atoms/animations/loading-animation.vue";
import AppButton from "~/components/_atoms/ui/app-button.vue";

const props = defineProps<{
  columns: TableColumn<T>[];
  data: T[];
  loading?: boolean;
  error?: boolean;
  errorTitle?: string;
  errorMessage?: string;
  emptyIcon?: string;
  emptyTitle?: string;
  loadingMessage?: string;
  /** When true, an empty result set means "no matches for the active filters", not "no data yet". */
  filtered?: boolean;
  filteredTitle?: string;
  /** Opt-in row virtualization. Height-bounds the root, so the table scrolls internally instead of growing the page. */
  virtualize?: boolean;
  /** Gates scroll-driven load-more. Pass `hasMore && !loading` so a page in flight can't re-fire it. */
  canLoadMore?: boolean;
}>();

const emit = defineEmits<{ retry: [], clearFilters: [], loadMore: [] }>();

const slots = defineSlots<Record<string, (scope: any) => any>>();
const {t} = useI18n();

const tableRef = useTemplateRef<{ $el?: HTMLElement }>('tableRef');

const ROW_HEIGHT = 65;

const virtualizeOptions = props.virtualize ? {estimateSize: ROW_HEIGHT} : false;
const tableUi = {
  root: `rounded-lg overflow-x-auto ring ring-default bg-default${props.virtualize ? ' max-h-[70vh] overflow-y-auto' : ''}`,
};

useInfiniteScroll(
  () => tableRef.value?.$el,
  () => emit('loadMore'),
  {distance: 200, throttle: 100, canLoadMore: () => props.canLoadMore === true},
);
</script>

<template>
  <UTable
    ref="tableRef"
    :columns="columns"
    :data="data"
    :loading="loading"
    :virtualize="virtualizeOptions"
    :ui="tableUi"
  >
    <template v-for="(_, name) in slots" :key="name" #[name]="scope">
      <slot :name="name" v-bind="scope"/>
    </template>

    <template v-if="!slots.empty" #empty>
      <div v-if="error" class="flex flex-col items-center justify-center py-10 gap-3">
        <UIcon class="w-8 h-8 text-error" name="i-lucide-triangle-alert"/>
        <p class="text-sm font-medium">{{ errorTitle ?? t('common.states.error') }}</p>
        <p v-if="errorMessage" class="text-sm text-muted">{{ errorMessage }}</p>
        <AppButton color="neutral" variant="soft" icon="i-lucide-refresh-cw" size="sm" @click="emit('retry')">
          {{ t('common.actions.retry') }}
        </AppButton>
      </div>
      <div v-else-if="filtered" class="flex flex-col items-center justify-center py-10 gap-3">
        <UIcon class="w-8 h-8 text-dimmed" name="i-lucide-search-x"/>
        <p class="text-sm text-muted">{{ filteredTitle ?? t('common.states.noResults') }}</p>
        <AppButton color="neutral" variant="soft" icon="i-lucide-x" size="sm" @click="emit('clearFilters')">
          {{ t('common.actions.clearFilters') }}
        </AppButton>
      </div>
      <div v-else class="flex flex-col items-center justify-center py-10 gap-3">
        <UIcon class="w-8 h-8 text-dimmed" :name="emptyIcon ?? 'i-lucide-inbox'"/>
        <p class="text-sm text-muted">{{ emptyTitle ?? t('transactions.table_meta.emptyDefault') }}</p>
      </div>
    </template>

    <template v-if="!slots.loading" #loading>
      <div class="flex flex-col items-center justify-center py-10 gap-3">
        <LoadingAnimation/>
        <p v-if="loadingMessage" class="text-sm text-muted">{{ loadingMessage }}</p>
      </div>
    </template>
  </UTable>
</template>
