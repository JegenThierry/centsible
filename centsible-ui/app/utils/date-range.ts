import {endOfMonth, endOfYear, format, startOfMonth, startOfYear, subMonths} from 'date-fns';
import {ISO_DATE} from "~/utils/date";

export type DateRangePreset = 'CURRENT_MONTH' | 'PAST_3_MONTHS' | 'YEAR' | 'ALL_TIME' | 'CUSTOM';

export const DATE_RANGE_PRESETS: DateRangePreset[] = [
  'CURRENT_MONTH',
  'PAST_3_MONTHS',
  'YEAR',
  'ALL_TIME',
  'CUSTOM',
];

export interface IsoDateRange {
  fromDate: string | null;
  toDate: string | null;
}

export interface DateWindow extends IsoDateRange {
  from: Date | null;
  to: Date | null;
  months: number;
}

function iso(date: Date): string {
  return format(date, ISO_DATE);
}

export function resolveWindow(preset: Exclude<DateRangePreset, 'CUSTOM'>, reference: Date = new Date()): DateWindow {
  switch (preset) {
    case 'CURRENT_MONTH': {
      const from = startOfMonth(reference);
      const to = endOfMonth(reference);
      return {from, to, fromDate: iso(from), toDate: iso(to), months: 1};
    }
    case 'PAST_3_MONTHS': {
      const from = startOfMonth(subMonths(reference, 2));
      const to = endOfMonth(reference);
      return {from, to, fromDate: iso(from), toDate: iso(to), months: 3};
    }
    case 'YEAR': {
      const from = startOfYear(reference);
      const to = endOfYear(reference);
      return {from, to, fromDate: iso(from), toDate: iso(to), months: 12};
    }
    case 'ALL_TIME': {
      const from = new Date(1970, 0, 1);
      const to = endOfYear(reference);
      // `from`/`to` left null so client-side range tests treat ALL_TIME as "match everything".
      return {from: null, to: null, fromDate: iso(from), toDate: iso(to), months: 36};
    }
  }
}

export function resolvePreset(preset: DateRangePreset, reference: Date = new Date()): IsoDateRange {
  if (preset === 'CUSTOM') return {fromDate: null, toDate: null};
  const w = resolveWindow(preset, reference);
  return preset === 'ALL_TIME' ? {fromDate: null, toDate: null} : {fromDate: w.fromDate, toDate: w.toDate};
}
