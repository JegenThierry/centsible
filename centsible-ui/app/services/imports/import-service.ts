import type {AxiosInstance} from "axios";
import {postMultipart, validateRequest} from "~/composables/use-api";
import {invalidateLedgerAggregates} from "~/utils/ledger-aggregates";
import type {
  CsvProbeResponse,
  ImportDetection,
  ImportPreview,
  ImportResult,
  ParseHints,
  ParserSummary,
  CsvProfileSummary,
  ImportMappingTemplate,
  ImportMappingTemplateForm,
} from "~/models/imports/imports";

/**
 * Thin wrapper around /api/imports/*. The wizard component drives:
 *   1. `detect(file)` — server tells us which parser owns this file (CSV, OFX, ...)
 *   2. (CSV only) `csvProbe(file)` — server returns header + suggested bank profile + mapping
 *   3. `preview(file, parserId, hints)` — parse without writing, for the confirmation screen
 *   4. `commit(accountId, file, parserId, hints)` — parse + persist via importBatch
 */
export function useImportService(api: AxiosInstance) {

  async function detect(file: File): Promise<ImportDetection> {
    return postMultipart<ImportDetection>(api, `/imports/detect`, {file});
  }

  async function csvProbe(file: File): Promise<CsvProbeResponse> {
    return postMultipart<CsvProbeResponse>(api, `/imports/csv/probe`, {file});
  }

  async function preview(
    file: File,
    parserId: string,
    hints: ParseHints,
    maxRows: number = 50,
  ): Promise<ImportPreview> {
    return postMultipart<ImportPreview>(api, `/imports/preview`, {
      file,
      parserId,
      hints: JSON.stringify(hints),
      maxRows: String(maxRows),
    });
  }

  async function commit(
    accountId: string,
    file: File,
    parserId: string,
    hints: ParseHints,
  ): Promise<ImportResult> {
    const result = await postMultipart<ImportResult>(api, `/imports/${encodeURIComponent(accountId)}`, {
      file,
      parserId,
      hints: JSON.stringify(hints),
    });
    invalidateLedgerAggregates();
    return result;
  }

  async function listProfiles(): Promise<CsvProfileSummary[]> {
    const response = await api.get<CsvProfileSummary[]>(`/imports/profiles`);
    return validateRequest<CsvProfileSummary[]>(response);
  }

  async function listParsers(): Promise<ParserSummary[]> {
    const response = await api.get<ParserSummary[]>(`/imports/parsers`);
    return validateRequest<ParserSummary[]>(response);
  }

  async function listTemplates(): Promise<ImportMappingTemplate[]> {
    const response = await api.get<ImportMappingTemplate[]>(`/imports/templates`);
    return validateRequest<ImportMappingTemplate[]>(response);
  }

  async function createTemplate(form: ImportMappingTemplateForm): Promise<ImportMappingTemplate> {
    const response = await api.post<ImportMappingTemplate>(`/imports/templates`, form);
    return validateRequest<ImportMappingTemplate>(response);
  }

  async function updateTemplate(id: string, form: ImportMappingTemplateForm): Promise<ImportMappingTemplate> {
    const response = await api.put<ImportMappingTemplate>(`/imports/templates/${encodeURIComponent(id)}`, form);
    return validateRequest<ImportMappingTemplate>(response);
  }

  async function deleteTemplate(id: string): Promise<void> {
    await api.delete(`/imports/templates/${encodeURIComponent(id)}`);
  }

  return {
    detect,
    csvProbe,
    preview,
    commit,
    listProfiles,
    listParsers,
    listTemplates,
    createTemplate,
    updateTemplate,
    deleteTemplate,
  };
}
