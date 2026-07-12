import {computed} from 'vue';
import {differenceInCalendarMonths} from 'date-fns';
import {monthsAgoIsoDate, todayIsoDate} from "~/utils/date";

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
  const state = useLocalStorage(
    `${STORAGE_KEY_PREFIX}${storageKey}`,
    {
      preset: defaultPreset,
      from: '',
      to: todayIsoDate(),
    },
    {mergeDefaults: true},
  );

  const preset = computed<ReportRangePreset>({
    get: () => state.value.preset,
    set: (v) => { state.value.preset = v; },
  });
  const customFrom = computed<string>({
    get: () => state.value.from,
    set: (v) => { state.value.from = v; },
  });
  const customTo = computed<string>({
    get: () => state.value.to,
    set: (v) => { state.value.to = v; },
  });

  const resolved = computed<ResolvedReportRange>(() => {
    if (preset.value === 'custom') {
      const from = customFrom.value || monthsAgoIsoDate(6);
      const to = customTo.value || todayIsoDate();
      const months = monthsBetween(from, to);
      return {startDate: from, endDate: to, months, preset: 'custom'};
    }
    const found = REPORT_RANGE_PRESETS.find(p => p.key === preset.value) ?? REPORT_RANGE_PRESETS[1]!;
    return {
      startDate: monthsAgoIsoDate(found.months),
      endDate: todayIsoDate(),
      months: found.months,
      preset: preset.value,
    };
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
  return Math.max(1, differenceInCalendarMonths(to, from));
}
