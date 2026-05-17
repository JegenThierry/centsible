import {computed, ref} from 'vue';
import {endOfMonth, endOfYear, format, parseISO, startOfMonth, startOfYear, subMonths} from 'date-fns';
import {ISO_DATE} from "~/utils/date";

export type DashboardPeriod = 'CURRENT_MONTH' | 'PAST_3_MONTHS' | 'YEAR' | 'ALL_TIME';

export const DASHBOARD_PERIODS: DashboardPeriod[] = ['CURRENT_MONTH', 'PAST_3_MONTHS', 'YEAR', 'ALL_TIME'];

export interface PeriodWindow {
  from: Date | null;
  to: Date | null;
  fromIso: string | null;
  toIso: string | null;
  months: number;
}

const period = ref<DashboardPeriod>('CURRENT_MONTH');

function iso(date: Date): string {
  return format(date, ISO_DATE);
}

export function resolvePeriod(value: DashboardPeriod, now: Date = new Date()): PeriodWindow {
  switch (value) {
    case 'CURRENT_MONTH':
      return {
        from: startOfMonth(now),
        to: endOfMonth(now),
        fromIso: iso(startOfMonth(now)),
        toIso: iso(endOfMonth(now)),
        months: 1,
      };
    case 'PAST_3_MONTHS': {
      const from = startOfMonth(subMonths(now, 2));
      const to = endOfMonth(now);
      return {from, to, fromIso: iso(from), toIso: iso(to), months: 3};
    }
    case 'YEAR': {
      const from = startOfYear(now);
      const to = endOfYear(now);
      return {from, to, fromIso: iso(from), toIso: iso(to), months: 12};
    }
    case 'ALL_TIME': {
      const from = new Date(1970, 0, 1);
      const to = endOfYear(now);
      return {from: null, to: null, fromIso: iso(from), toIso: iso(to), months: 36};
    }
  }
}

export function useDashboardPeriod() {
  const window = computed<PeriodWindow>(() => resolvePeriod(period.value));

  function isInPeriod(isoDate: string): boolean {
    if (!window.value.from || !window.value.to) return true;
    const d = parseISO(isoDate);
    return d.getTime() >= window.value.from.getTime() && d.getTime() <= window.value.to.getTime();
  }

  return {period, window, isInPeriod};
}
