import type {ExportFormat, ExportType} from "~/models/export/export-job";

export type ScheduleFrequency = 'DAILY' | 'WEEKLY' | 'MONTHLY' | 'YEARLY';

/** Frequencies offered in the UI. DAILY is accepted by the backend but omitted as too noisy for reports. */
export const SCHEDULE_FREQUENCIES: ScheduleFrequency[] = ['WEEKLY', 'MONTHLY', 'YEARLY'];

/** A recurring export. Each run emails the just-ended period (monthly → last month, weekly → last week). */
export interface ExportSchedule {
  id: string;
  type: ExportType;
  format: ExportFormat;
  title: string;
  frequency: ScheduleFrequency;
  nextRunAt?: string;
  active: boolean;
  lastRunAt?: string;
  createdAt?: string;
  modifiedAt?: string;
}

export interface ExportScheduleForm {
  title: string;
  format: ExportFormat;
  frequency: ScheduleFrequency;
}

export interface ExportScheduleUpdateForm {
  title: string;
  format: ExportFormat;
  frequency: ScheduleFrequency;
  active: boolean;
}
