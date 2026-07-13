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

const FORMATTERS: Record<DateFormat, (d: Date, tag: string, hasTime: boolean, showYear: boolean) => string> = {
  short: (d, tag) => d.toLocaleDateString(tag, {day: '2-digit', month: '2-digit'}),
  long:  (d, tag, _hasTime, showYear) => d.toLocaleDateString(tag, {day: 'numeric', month: 'short', ...(showYear ? {year: 'numeric'} : {})}),
  date:  (d, tag) => d.toLocaleDateString(tag, {day: 'numeric', month: 'short', year: 'numeric'}),
  time:  (d, tag) => d.toLocaleTimeString(tag, {hour: '2-digit', minute: '2-digit', hour12: false}),
  full:  (d, tag, hasTime, showYear) => hasTime
    ? d.toLocaleString(tag, {day: 'numeric', month: 'short', ...(showYear ? {year: 'numeric'} : {}), hour: '2-digit', minute: '2-digit', hour12: false})
    : d.toLocaleDateString(tag, {day: 'numeric', month: 'short', ...(showYear ? {year: 'numeric'} : {})}),
};

const DATE_ONLY = /^(\d{4})-(\d{2})-(\d{2})$/;

function toLocalDate(value: string | Date): Date {
  if (value instanceof Date) return value;
  const m = DATE_ONLY.exec(value);
  return m ? new Date(Number(m[1]), Number(m[2]) - 1, Number(m[3])) : new Date(value);
}

function hasTimeComponent(value: string | Date): boolean {
  if (value instanceof Date) return true;
  return !DATE_ONLY.test(value);
}

const formattedDate = computed(() => {
  const d = toLocalDate(props.date);
  const showYear = d.getFullYear() !== new Date().getFullYear();
  return FORMATTERS[props.format](d, activeLocale.value, hasTimeComponent(props.date), showYear);
});
</script>

<template>
  <span>{{ formattedDate }}</span>
</template>
