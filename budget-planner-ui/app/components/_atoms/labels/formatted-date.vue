<script lang="ts" setup>
const props = withDefaults(defineProps<{
  date: string | Date;
  format?: 'short' | 'long' | 'date' | 'time' | 'full';
  locale?: string;
}>(), {
  format: 'full'
});

const localeTag = useLocaleTag();
const activeLocale = computed(() => props.locale ?? localeTag.value);

const formattedDate = computed(() => {
  const d = new Date(props.date);
  const tag = activeLocale.value;
  if (props.format === 'short') {
    return d.toLocaleDateString(tag, {day: '2-digit', month: '2-digit'});
  }
  if (props.format === 'long') {
    return d.toLocaleDateString(tag, {day: 'numeric', month: 'short'});
  }
  if (props.format === 'date') {
    return d.toLocaleDateString(tag, {day: 'numeric', month: 'short', year: 'numeric'});
  }
  if (props.format === 'time') {
    return d.toLocaleTimeString(tag, {hour: '2-digit', minute: '2-digit', hour12: false});
  }
  return d.toLocaleString(tag, {
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
