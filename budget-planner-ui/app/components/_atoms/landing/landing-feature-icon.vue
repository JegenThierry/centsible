<script lang="ts" setup>
defineProps<{
  icon: string;
  colorClass?: string;
}>();
</script>

<template>
  <div
    :class="[
      'flex items-center justify-center w-11 h-11 rounded-xl shrink-0',
      colorClass ?? 'bg-primary/10 text-primary'
    ]">
    <UIcon class="w-6 h-6" :name="icon"/>
  </div>
</template>
