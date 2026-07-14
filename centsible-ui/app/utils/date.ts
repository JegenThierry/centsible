import {differenceInCalendarDays, format, subDays, subMonths} from 'date-fns';

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

/**
 * The equal-length ISO date range immediately preceding [startDate, endDate], for period-over-period
 * comparisons (e.g. this 6 months vs the previous 6 months).
 */
export function previousIsoDateRange(startDate: string, endDate: string): {startDate: string; endDate: string} {
  const start = new Date(startDate);
  const spanDays = differenceInCalendarDays(new Date(endDate), start);
  const prevEnd = subDays(start, 1);
  const prevStart = subDays(prevEnd, spanDays);
  return {startDate: format(prevStart, ISO_DATE), endDate: format(prevEnd, ISO_DATE)};
}

/** Formats an ISO timestamp as a localized relative phrase, e.g. "3 months ago" / "il y a 3 mois". */
export function formatRelativeToNow(iso: string, localeTag: string): string {
  const rtf = new Intl.RelativeTimeFormat(localeTag, {numeric: 'auto'});
  const divisions: Array<[number, Intl.RelativeTimeFormatUnit]> = [
    [60, 'second'], [60, 'minute'], [24, 'hour'], [7, 'day'], [4.34524, 'week'], [12, 'month'],
  ];
  let duration = (new Date(iso).getTime() - Date.now()) / 1000;
  for (const [amount, unit] of divisions) {
    if (Math.abs(duration) < amount) return rtf.format(Math.round(duration), unit);
    duration /= amount;
  }
  return rtf.format(Math.round(duration), 'year');
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
