import {format, subDays, subMonths} from 'date-fns';

export const ISO_DATE = 'yyyy-MM-dd';

export function todayIsoDate(): string {
  return format(new Date(), ISO_DATE);
}

export function monthsAgoIsoDate(months: number, reference: Date = new Date()): string {
  return format(subMonths(reference, months), ISO_DATE);
}

export function daysAgoIsoDate(days: number, reference: Date = new Date()): string {
  return format(subDays(reference, days), ISO_DATE);
}

export function isoDateRangeForMonthsBack(months: number): {startDate: string; endDate: string} {
  return {startDate: monthsAgoIsoDate(months), endDate: todayIsoDate()};
}
