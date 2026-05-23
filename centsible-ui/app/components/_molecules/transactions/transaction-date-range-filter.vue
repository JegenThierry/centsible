<script lang="ts" setup>
import {computed} from 'vue';
import FilterPopoverButton from "~/components/_molecules/inputs/filter-popover-button.vue";

const fromDate = defineModel<string>('fromDate', {required: true});
const toDate = defineModel<string>('toDate', {required: true});

const {t} = useI18n();

const active = computed(() => !!fromDate.value || !!toDate.value);

const buttonLabel = computed(() => {
  if (fromDate.value && toDate.value) return `${fromDate.value} → ${toDate.value}`;
  if (fromDate.value) return t('transactions.filters.fromShort', {date: fromDate.value});
  if (toDate.value) return t('transactions.filters.toShort', {date: toDate.value});
  return t('transactions.filters.dateRangeLabel');
});

function clear() {
  fromDate.value = '';
  toDate.value = '';
}
</script>

<template>
  <FilterPopoverButton :active="active"
                       :label="buttonLabel"
                       icon="i-lucide-calendar">
    <div class="w-64 space-y-3">
      <div class="flex items-center justify-between">
        <span class="text-xs font-semibold uppercase text-muted">{{ t('transactions.filters.dateRangeLabel') }}</span>
        <UButton v-if="active"
                 color="neutral"
                 size="xs"
                 variant="ghost"
                 @click="clear">
          {{ t('transactions.filters.clear') }}
        </UButton>
      </div>

      <div class="space-y-2">
        <label class="block text-xs font-medium text-muted">{{ t('transactions.filters.fromLabel') }}</label>
        <UInput v-model="fromDate" class="w-full" type="date"/>
      </div>

      <div class="space-y-2">
        <label class="block text-xs font-medium text-muted">{{ t('transactions.filters.toLabel') }}</label>
        <UInput v-model="toDate" class="w-full" type="date"/>
      </div>
    </div>
  </FilterPopoverButton>
</template>
