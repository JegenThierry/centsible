<script lang="ts">
const SELECT_MENU_UI = {
  base: 'group w-fit cursor-pointer rounded-md -mx-1 px-1 py-0.5 hover:bg-elevated focus-visible:outline-2 focus-visible:outline-primary',
  trailingIcon: 'hidden',
  content: 'min-w-56',
} as const;
</script>

<script lang="ts" setup>
import type {Category} from "~/models/category/category";
import CategoryBadge from "~/components/_molecules/badges/category-badge.vue";

const props = defineProps<{
  category?: Category | null;
  options: Category[];
}>();

const emit = defineEmits<{
  (e: 'select', category: Category): void;
}>();

const {t} = useI18n();

const selected = computed(() => props.options.find((c) => c.id === props.category?.id));

function onSelect(category: Category | undefined) {
  if (category && category.id !== props.category?.id) emit('select', category);
}
</script>

<template>
  <USelectMenu :model-value="selected"
               :items="options"
               :aria-label="t('transactions.category.editAria')"
               :ui="SELECT_MENU_UI"
               label-key="name"
               searchable
               variant="none"
               @update:model-value="onSelect">
    <template #default>
      <span class="inline-flex items-center gap-1">
        <CategoryBadge :color="category?.color" :icon="category?.icon" :name="category?.name"/>
        <UIcon class="w-3 h-3 shrink-0 text-dimmed transition-colors group-hover:text-default"
               name="i-lucide-chevron-down"/>
      </span>
    </template>

    <template #item-leading="{ item }">
      <UIcon :name="item.icon" :style="{ color: item.color }" class="w-4 h-4 flex my-auto"/>
    </template>
  </USelectMenu>
</template>
