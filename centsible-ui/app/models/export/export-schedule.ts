import type {ExportFormat, ExportType} from "~/models/export/export-job";

export type ScheduleFrequency = 'DAILY' | 'WEEKLY' | 'MONTHLY' | 'YEARLY';

export const SCHEDULE_FREQUENCIES: ScheduleFrequency[] = ['WEEKLY', 'MONTHLY', 'YEARLY'];

export const EXPORT_SCHEDULE_TYPES: ExportType[] = ['TRANSACTIONS', 'LENDINGS_ALL', 'ACCOUNTS_SUMMARY'];

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
  type?: ExportType;
  format: ExportFormat;
  frequency: ScheduleFrequency;
}

export interface ExportScheduleUpdateForm {
  title: string;
  type?: ExportType;
  format: ExportFormat;
  frequency: ScheduleFrequency;
  active: boolean;
}
