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

function iso(date: Date): string {
  return format(date, ISO_DATE);
}

export function resolvePreset(preset: DateRangePreset, reference: Date = new Date()): IsoDateRange {
  switch (preset) {
    case 'CURRENT_MONTH':
      return {fromDate: iso(startOfMonth(reference)), toDate: iso(endOfMonth(reference))};
    case 'PAST_3_MONTHS':
      return {fromDate: iso(startOfMonth(subMonths(reference, 2))), toDate: iso(endOfMonth(reference))};
    case 'YEAR':
      return {fromDate: iso(startOfYear(reference)), toDate: iso(endOfYear(reference))};
    case 'ALL_TIME':
    case 'CUSTOM':
      return {fromDate: null, toDate: null};
  }
}
