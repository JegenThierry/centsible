<script lang="ts" setup>
defineProps<{
  label: string;
  icon: string;
}>();
</script>

<template>
  <UBadge color="neutral" variant="subtle" size="lg">
    <UIcon class="w-4 h-4" :name="icon"/>
    {{ label }}
  </UBadge>
</template>
