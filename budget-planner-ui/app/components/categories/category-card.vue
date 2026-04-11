<script lang="ts" setup>
import type {Category} from "~/models/category/category";
import CategoryTypeBadge from "~/components/_molecules/badges/category-type-badge.vue";

defineProps<{
  category: Category;
}>();

const emit = defineEmits<{
  edit: [category: Category];
  delete: [category: Category];
}>();
</script>

<template>
  <UCard
    :class="[
      'group transition-all',
      category.system
        ? 'bg-neutral-50/50 dark:bg-neutral-900/30 border-neutral-200 dark:border-neutral-800'
        : 'border-primary-100 dark:border-primary-900/30'
    ]"
  >
    <div class="flex items-center justify-between">
      <div class="flex items-center gap-3">
        <div
          :class="[
            'flex p-2 rounded-lg my-auto',
            category.system
              ? 'bg-neutral-100 dark:bg-neutral-800 text-neutral-500 dark:text-neutral-400'
              : 'bg-primary-50 dark:bg-primary-900/20 text-primary-600 dark:text-primary-400'
          ]"
        >
          <UIcon :name="category.icon" class="w-6 h-6"/>
        </div>
        <div>
          <p class="font-semibold">{{ category.name }}</p>
          <div class="flex gap-2">
            <CategoryTypeBadge :type="category.type" />
            <UBadge v-if="category.system" color="neutral" variant="outline" size="xs">System</UBadge>
          </div>
        </div>
      </div>
      <div v-if="!category.system" class="flex gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
        <UButton
          icon="i-lucide-pencil"
          variant="ghost"
          color="neutral"
          size="sm"
          @click="emit('edit', category)"
        />
        <UButton
          icon="i-lucide-trash"
          variant="ghost"
          color="error"
          size="sm"
          @click="emit('delete', category)"
        />
      </div>
    </div>
  </UCard>
</template>
