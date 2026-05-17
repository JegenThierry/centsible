<script lang="ts" setup>
type DateFormat = 'short' | 'long' | 'date' | 'time' | 'full';

const props = withDefaults(defineProps<{
  date: string | Date;
  format?: DateFormat;
  locale?: string;
}>(), {
  format: 'full'
});

const localeTag = useLocaleTag();
const activeLocale = computed(() => props.locale ?? localeTag.value);

const FORMATTERS: Record<DateFormat, (d: Date, tag: string) => string> = {
  short: (d, tag) => d.toLocaleDateString(tag, {day: '2-digit', month: '2-digit'}),
  long:  (d, tag) => d.toLocaleDateString(tag, {day: 'numeric', month: 'short'}),
  date:  (d, tag) => d.toLocaleDateString(tag, {day: 'numeric', month: 'short', year: 'numeric'}),
  time:  (d, tag) => d.toLocaleTimeString(tag, {hour: '2-digit', minute: '2-digit', hour12: false}),
  full:  (d, tag) => d.toLocaleString(tag, {day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit', hour12: false}),
};

const formattedDate = computed(() => FORMATTERS[props.format](new Date(props.date), activeLocale.value));
</script>

<template>
  <span>{{ formattedDate }}</span>
</template>
