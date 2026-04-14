<script setup lang="ts">
const props = withDefaults(defineProps<{
  date: string | Date;
  format?: 'short' | 'long' | 'time' | 'full';
  locale?: string;
}>(), {
  format: 'full',
  locale: 'de-DE'
});

const formattedDate = computed(() => {
  const d = new Date(props.date);
  if (props.format === 'short') {
    return d.toLocaleDateString(props.locale, {day: '2-digit', month: '2-digit'});
  }
  if (props.format === 'long') {
    return d.toLocaleDateString(props.locale, {day: 'numeric', month: 'short'});
  }
  if (props.format === 'time') {
    return d.toLocaleTimeString(props.locale, {hour: '2-digit', minute: '2-digit', hour12: false});
  }
  return d.toLocaleString(props.locale, {
    day: 'numeric',
    month: 'short',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false
  });
});
</script>

<template>
  <span>{{ formattedDate }}</span>
</template>
