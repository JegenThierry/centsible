import {computed, ref} from 'vue';
import {parseISO} from 'date-fns';
import {type DateWindow, resolveWindow} from "~/utils/date-range";

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

function toPeriodWindow(w: DateWindow): PeriodWindow {
  return {from: w.from, to: w.to, fromIso: w.fromDate, toIso: w.toDate, months: w.months};
}

export function resolvePeriod(value: DashboardPeriod, now: Date = new Date()): PeriodWindow {
  return toPeriodWindow(resolveWindow(value, now));
}

export function useDashboardPeriod() {
  const window = computed<PeriodWindow>(() => resolvePeriod(period.value));
  return {period, window};
}
