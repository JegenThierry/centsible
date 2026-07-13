<script lang="ts" setup>
import {computed, onMounted} from 'vue';
import FilterPopoverButton from "~/components/_molecules/inputs/filter-popover-button.vue";
import {useTagsStore} from "~/stores/tagsStore";
import AppButton from "~/components/_atoms/ui/app-button.vue";

const model = defineModel<number[]>({required: true});

const {t} = useI18n();
const tagsStore = useTagsStore();

const buttonLabel = computed(() => {
  if (model.value.length === 1) {
    const name = tagsStore.tags.find((tag) => tag.id === model.value[0])?.name;
    return name ?? t('transactions.filters.tagLabel');
  }
  return t('transactions.filters.tagLabel');
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
  if (tagsStore.tags.length === 0) tagsStore.fetchAll();
});
</script>

<template>
  <FilterPopoverButton :active="model.length > 0"
                       :count="model.length"
                       :label="buttonLabel"
                       icon="i-lucide-tags">
    <div class="w-64 max-h-80 flex flex-col">
      <div class="flex items-center justify-between pb-2 mb-1 border-b border-default">
        <span class="text-xs font-semibold uppercase text-muted">{{ t('transactions.filters.tagLabel') }}</span>
        <AppButton v-if="model.length > 0"
                 color="neutral"
                 size="xs"
                 variant="ghost"
                 @click="clear">
          {{ t('transactions.filters.clear') }}
        </AppButton>
      </div>

      <div v-if="tagsStore.tags.length === 0" class="py-4 text-sm text-muted text-center">
        {{ t('transactions.filters.noTags') }}
      </div>

      <ul class="flex-1 overflow-y-auto space-y-0.5">
        <li v-for="tag in tagsStore.tags" :key="tag.id">
          <label class="flex items-center gap-2 px-2 py-1.5 rounded hover:bg-elevated cursor-pointer">
            <input :checked="model.includes(tag.id)"
                   class="cursor-pointer"
                   type="checkbox"
                   @change="(e) => toggle(tag.id, (e.target as HTMLInputElement).checked)"/>
            <span :style="{backgroundColor: tag.color}" class="w-3 h-3 rounded-full shrink-0"/>
            <span class="text-sm truncate">{{ tag.name }}</span>
          </label>
        </li>
      </ul>
    </div>
  </FilterPopoverButton>
</template>
