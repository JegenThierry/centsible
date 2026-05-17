<script lang="ts" setup generic="T">
import type {TableColumn} from '@nuxt/ui';
import LoadingAnimation from "~/components/_atoms/animations/loading-animation.vue";

defineProps<{
  columns: TableColumn<T>[];
  data: T[];
  loading?: boolean;
  emptyIcon?: string;
  emptyTitle?: string;
  loadingMessage?: string;
}>();

const slots = defineSlots<Record<string, (scope: any) => any>>();
const {t} = useI18n();
</script>

<template>
  <UTable
    :columns="columns"
    :data="data"
    :loading="loading"
    :ui="{ root: 'rounded-lg overflow-x-auto ring ring-default bg-default' }"
  >
    <template v-for="(_, name) in slots" :key="name" #[name]="scope">
      <slot :name="name" v-bind="scope"/>
    </template>

    <template v-if="!slots.empty" #empty>
      <div class="flex flex-col items-center justify-center py-10 gap-3">
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
