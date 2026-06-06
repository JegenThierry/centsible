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

const DATE_ONLY = /^(\d{4})-(\d{2})-(\d{2})$/;

// Date-only ISO strings (LocalDate fields like transactionDate / loanDate / dueDate) must be read in
// local time. `new Date('2026-06-06')` parses as UTC midnight, so toLocale* renders a day early west
// of UTC; build a local Date from the parts instead. Datetime strings (with a time/offset) and Date
// objects are passed through unchanged.
function toLocalDate(value: string | Date): Date {
  if (value instanceof Date) return value;
  const m = DATE_ONLY.exec(value);
  return m ? new Date(Number(m[1]), Number(m[2]) - 1, Number(m[3])) : new Date(value);
}

const formattedDate = computed(() => FORMATTERS[props.format](toLocalDate(props.date), activeLocale.value));
</script>

<template>
  <span>{{ formattedDate }}</span>
</template>
