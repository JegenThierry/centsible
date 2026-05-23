<script lang="ts" setup>
import type {Category} from "~/models/category/category";
import CategoryTypeBadge from "~/components/_molecules/badges/category-type-badge.vue";
import CategoryIcon from "~/components/_atoms/categories/category-icon.vue";
import EditDeleteActions from "~/components/_molecules/buttons/edit-delete-actions.vue";

defineProps<{
  category: Category;
}>();

const emit = defineEmits<{
  edit: [category: Category];
  delete: [category: Category];
}>();

const {t} = useI18n();
</script>

<template>
  <UCard
    :class="[
      'group transition-all',
      category.system
        ? 'bg-muted border-default'
        : 'border-primary-200 dark:border-primary-900/40'
    ]"
  >
    <div class="flex items-center justify-between">
      <div class="flex items-center gap-3">
        <CategoryIcon :color="category.color" :icon="category.icon" size="lg"/>
        <div>
          <p class="font-semibold">{{ category.name }}</p>
          <div class="flex gap-2">
            <CategoryTypeBadge :type="category.type"/>
            <UBadge v-if="category.system" color="neutral" size="xs" variant="outline">{{ t('common.category.system') }}</UBadge>
          </div>
        </div>
      </div>
      <EditDeleteActions v-if="!category.system"
                         hide-on-hover
                         @edit="emit('edit', category)"
                         @delete="emit('delete', category)"/>
    </div>
  </UCard>
</template>
