<script lang="ts" setup>
defineProps<{
  title: string;
  icon: string;
}>();
</script>

<template>
  <div class="flex items-center gap-2">
    <UIcon class="w-5 h-5 text-primary" :name="icon"/>
    <h3 class="font-semibold text-highlighted">{{ title }}</h3>
  </div>
</template>
