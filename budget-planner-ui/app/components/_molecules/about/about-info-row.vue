<script lang="ts" setup>
defineProps<{
  label: string;
}>();
</script>

<template>
  <div class="flex items-center justify-between py-3 first:pt-0 last:pb-0">
    <dt class="text-sm text-muted">{{ label }}</dt>
    <dd class="text-sm font-medium text-highlighted">
      <slot/>
    </dd>
  </div>
</template>
