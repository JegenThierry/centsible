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

/** Formats a "yyyy-MM" key as a localized month + year, e.g. "June 2026" / "juin 2026". */
export function formatMonthYearLabel(
  yearMonth: string,
  localeTag: string,
  options: Intl.DateTimeFormatOptions = {month: 'long', year: 'numeric'},
): string {
  const [year, month] = yearMonth.split('-').map(Number);
  return new Intl.DateTimeFormat(localeTag, options).format(new Date(year!, month! - 1, 1));
}
