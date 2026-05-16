<script lang="ts" setup>
import type {Category} from "~/models/category/category";
import CategoryTypeBadge from "~/components/_molecules/badges/category-type-badge.vue";
import CategoryIcon from "~/components/_atoms/categories/category-icon.vue";

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
      <div v-if="!category.system" class="flex gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
        <UButton
          color="neutral"
          icon="i-lucide-pencil"
          size="sm"
          variant="ghost"
          @click="emit('edit', category)"
        />
        <UButton
          color="error"
          icon="i-lucide-trash"
          size="sm"
          variant="ghost"
          @click="emit('delete', category)"
        />
      </div>
    </div>
  </UCard>
</template>
