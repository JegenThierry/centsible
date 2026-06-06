<script lang="ts" setup>
type BadgeColor = 'primary' | 'secondary' | 'success' | 'info' | 'warning' | 'error' | 'neutral'

export interface IntegrationCardBadge {
  label: string
  color?: BadgeColor
}

defineProps<{
  title: string
  description?: string
  icon?: string
  badges?: IntegrationCardBadge[]
}>()
</script>

<template>
  <UCard class="h-full" :ui="{body: 'flex flex-col gap-3 h-full'}">
    <div>
      <div class="flex items-center gap-2 min-w-0">
        <UIcon v-if="icon" class="w-5 h-5 shrink-0" :name="icon"/>
        <h3 class="font-semibold truncate">{{ title }}</h3>
      </div>
      <p v-if="description" class="text-sm text-neutral-500 mt-1">{{ description }}</p>
    </div>

    <div v-if="badges && badges.length > 0" class="flex flex-wrap gap-1">
      <UBadge v-for="(b, i) in badges"
              :key="i"
              :color="b.color ?? 'neutral'"
              size="sm"
              variant="soft">
        {{ b.label }}
      </UBadge>
    </div>

    <slot/>

    <div class="flex-1"/>

    <div v-if="$slots.footer" class="flex flex-wrap gap-2 [&>*]:flex-1">
      <slot name="footer"/>
    </div>
  </UCard>
</template>
