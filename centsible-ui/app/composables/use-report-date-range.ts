import {computed, ref, watch} from 'vue';
import {format, subMonths} from 'date-fns';
import {ISO_DATE, todayIsoDate} from "~/utils/date";

export type ReportRangePreset = '3m' | '6m' | '12m' | '24m' | 'custom';

export const REPORT_RANGE_PRESETS: ReadonlyArray<{key: ReportRangePreset; months: number}> = [
  {key: '3m', months: 3},
  {key: '6m', months: 6},
  {key: '12m', months: 12},
  {key: '24m', months: 24},
];

export interface ResolvedReportRange {
  startDate: string;
  endDate: string;
  /** Months-back equivalent, derived from custom from/to when needed. */
  months: number;
  preset: ReportRangePreset;
}

const STORAGE_KEY_PREFIX = 'centsible.reports.range.';

/**
 * Persists the user's selected range per surface (e.g. "reports" or "dashboard") so refreshing
 * the page doesn't reset their choice. Custom ranges store from/to dates; preset ranges store
 * the preset key.
 */
export function useReportDateRange(storageKey: string, defaultPreset: ReportRangePreset = '6m') {
  const preset = ref<ReportRangePreset>(defaultPreset);
  const customFrom = ref<string>('');
  const customTo = ref<string>(todayIsoDate());

  const storageId = `${STORAGE_KEY_PREFIX}${storageKey}`;

  if (typeof window !== 'undefined') {
    try {
      const raw = window.localStorage.getItem(storageId);
      if (raw) {
        const saved = JSON.parse(raw) as {preset?: ReportRangePreset; from?: string; to?: string};
        if (saved.preset) preset.value = saved.preset;
        if (saved.from) customFrom.value = saved.from;
        if (saved.to) customTo.value = saved.to;
      }
    } catch {
      // Ignore storage errors; defaults still apply.
    }
  }

  watch([preset, customFrom, customTo], () => {
    if (typeof window === 'undefined') return;
    try {
      window.localStorage.setItem(storageId, JSON.stringify({
        preset: preset.value,
        from: customFrom.value,
        to: customTo.value,
      }));
    } catch {
      // Quota errors etc. are non-fatal.
    }
  }, {deep: false});

  const resolved = computed<ResolvedReportRange>(() => {
    if (preset.value === 'custom') {
      const from = customFrom.value || format(subMonths(new Date(), 6), ISO_DATE);
      const to = customTo.value || todayIsoDate();
      // Approximate months span for callers that still take a months-back number.
      const months = monthsBetween(from, to);
      return {startDate: from, endDate: to, months, preset: 'custom'};
    }
    const found = REPORT_RANGE_PRESETS.find(p => p.key === preset.value) ?? REPORT_RANGE_PRESETS[1]!;
    const endDate = todayIsoDate();
    const startDate = format(subMonths(new Date(), found.months), ISO_DATE);
    return {startDate, endDate, months: found.months, preset: preset.value};
  });

  const isCustom = computed(() => preset.value === 'custom');
  const isCustomValid = computed(() => {
    if (!isCustom.value) return true;
    return Boolean(customFrom.value) && Boolean(customTo.value) && customFrom.value <= customTo.value;
  });

  return {
    preset,
    customFrom,
    customTo,
    resolved,
    isCustom,
    isCustomValid,
  };
}

function monthsBetween(fromIso: string, toIso: string): number {
  const from = new Date(fromIso);
  const to = new Date(toIso);
  if (Number.isNaN(from.getTime()) || Number.isNaN(to.getTime())) return 6;
  const diffMs = to.getTime() - from.getTime();
  return Math.max(1, Math.ceil(diffMs / (1000 * 60 * 60 * 24 * 30)));
}
