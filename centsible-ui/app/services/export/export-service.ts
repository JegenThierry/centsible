import type {AxiosInstance} from "axios";
import {assertStatus, validateRequest} from "~/composables/use-api";
import {triggerBrowserDownload} from "~/utils/blob-download";
import type {CreateExportRequest, ExportJob} from "~/models/export/export-job";
import type {ExportSchedule, ExportScheduleForm, ExportScheduleUpdateForm} from "~/models/export/export-schedule";

export function useExportService(api: AxiosInstance) {
  async function listExports(page: number = 1, size: number = 25): Promise<ExportJob[]> {
    const response = await api.get<ExportJob[]>(`/exports`, {params: {page, size}});
    return validateRequest<ExportJob[]>(response);
  }

  async function createExport(payload: CreateExportRequest): Promise<ExportJob> {
    const response = await api.post<ExportJob>(`/exports`, payload);
    return validateRequest<ExportJob>(response);
  }

  async function getExport(jobId: string): Promise<ExportJob> {
    const response = await api.get<ExportJob>(`/exports/${encodeURIComponent(jobId)}`);
    return validateRequest<ExportJob>(response);
  }

  async function retriggerExport(jobId: string): Promise<ExportJob> {
    const response = await api.post<ExportJob>(`/exports/${encodeURIComponent(jobId)}/retrigger`);
    return validateRequest<ExportJob>(response);
  }

  async function deleteExport(jobId: string): Promise<void> {
    assertStatus(await api.delete(`/exports/${encodeURIComponent(jobId)}`));
  }

  /** Re-wraps the response in a Blob keyed on the server's content-type so CSV/JSON exports aren't mislabelled as PDF. */
  async function downloadExport(jobId: string, filename?: string): Promise<void> {
    const response = await api.get(`/exports/${encodeURIComponent(jobId)}/download`, {
      responseType: 'blob',
    });
    const contentType = (response.headers['content-type'] as string | undefined) ?? 'application/octet-stream';
    const blob = new Blob([response.data], {type: contentType});
    triggerBrowserDownload(blob, filename || `export-${jobId}`);
  }

  async function listSchedules(): Promise<ExportSchedule[]> {
    const response = await api.get<ExportSchedule[]>(`/export-schedules`);
    return validateRequest<ExportSchedule[]>(response);
  }

  async function createSchedule(form: ExportScheduleForm): Promise<ExportSchedule> {
    const response = await api.post<ExportSchedule>(`/export-schedules`, form);
    return validateRequest<ExportSchedule>(response);
  }

  async function updateSchedule(id: string, form: ExportScheduleUpdateForm): Promise<ExportSchedule> {
    const response = await api.put<ExportSchedule>(`/export-schedules/${encodeURIComponent(id)}`, form);
    return validateRequest<ExportSchedule>(response);
  }

  async function deleteSchedule(id: string): Promise<void> {
    assertStatus(await api.delete(`/export-schedules/${encodeURIComponent(id)}`));
  }

  return {
    listExports,
    createExport,
    getExport,
    retriggerExport,
    deleteExport,
    downloadExport,
    listSchedules,
    createSchedule,
    updateSchedule,
    deleteSchedule,
  };
}
