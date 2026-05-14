import type {AxiosInstance} from "axios";
import {validateRequest} from "~/composables/use-api";
import type {CreateExportRequest, ExportJob} from "~/models/export/export-job";

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
    const response = await api.delete(`/exports/${encodeURIComponent(jobId)}`);
    if (response.status !== 204 && response.status !== 200) {
      throw new Error(response.statusText);
    }
  }

  async function downloadExport(jobId: string, filename?: string): Promise<void> {
    const response = await api.get(`/exports/${encodeURIComponent(jobId)}/download`, {
      responseType: 'blob',
    });
    const blob = new Blob([response.data], {type: 'application/pdf'});
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = filename || `export-${jobId}.pdf`;
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(url);
  }

  return {
    listExports,
    createExport,
    getExport,
    retriggerExport,
    deleteExport,
    downloadExport,
  };
}
