import {format} from 'date-fns';

export const ISO_DATE = 'yyyy-MM-dd';

export function todayIsoDate(): string {
  return format(new Date(), ISO_DATE);
}
