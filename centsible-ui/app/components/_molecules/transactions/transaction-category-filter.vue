<script lang="ts" setup>
import {computed, onMounted} from 'vue';
import FilterPopoverButton from "~/components/_molecules/inputs/filter-popover-button.vue";
import {useCategoriesStore} from "~/stores/categoriesStore";
import AppButton from "~/components/_atoms/ui/app-button.vue";

const model = defineModel<number[]>({required: true});

const {t} = useI18n();
const categoriesStore = useCategoriesStore();

const buttonLabel = computed(() => {
  if (model.value.length === 0) return t('transactions.filters.categoryLabel');
  if (model.value.length === 1) {
    const name = categoriesStore.categories.find((c) => c.id === model.value[0])?.name;
    return name ?? t('transactions.filters.categoryLabel');
  }
  return t('transactions.filters.categoryLabel');
});

function toggle(id: number, checked: boolean) {
  const set = new Set(model.value);
  if (checked) set.add(id); else set.delete(id);
  model.value = Array.from(set);
}

function clear() {
  model.value = [];
}

onMounted(() => {
  if (categoriesStore.categories.length === 0) categoriesStore.updateCategories();
});
</script>

<template>
  <FilterPopoverButton :active="model.length > 0"
                       :count="model.length"
                       :label="buttonLabel"
                       icon="i-lucide-tag">
    <div class="w-64 max-h-80 flex flex-col">
      <div class="flex items-center justify-between pb-2 mb-1 border-b border-default">
        <span class="text-xs font-semibold uppercase text-muted">{{ t('transactions.filters.categoryLabel') }}</span>
        <AppButton v-if="model.length > 0"
                 color="neutral"
                 size="xs"
                 variant="ghost"
                 @click="clear">
          {{ t('transactions.filters.clear') }}
        </AppButton>
      </div>

      <div v-if="categoriesStore.categories.length === 0" class="py-4 text-sm text-muted text-center">
        {{ t('transactions.filters.noCategories') }}
      </div>

      <ul class="flex-1 overflow-y-auto space-y-0.5">
        <li v-for="c in categoriesStore.categories" :key="c.id">
          <label class="flex items-center gap-2 px-2 py-1.5 rounded hover:bg-elevated cursor-pointer">
            <input :checked="model.includes(c.id)"
                   class="cursor-pointer"
                   type="checkbox"
                   @change="(e) => toggle(c.id, (e.target as HTMLInputElement).checked)"/>
            <UIcon :name="c.icon" :style="{color: c.color}" class="w-4 h-4 shrink-0"/>
            <span class="text-sm truncate">{{ c.name }}</span>
          </label>
        </li>
      </ul>
    </div>
  </FilterPopoverButton>
</template>
