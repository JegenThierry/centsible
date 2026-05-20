import type {AxiosInstance} from "axios";
import {validateRequest} from "~/composables/use-api";
import type {
  CsvProbeResponse,
  ImportDetection,
  ImportPreview,
  ImportResult,
  ParseHints,
  ParserSummary,
  CsvProfileSummary,
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
    const form = new FormData();
    form.append('file', file);
    const response = await api.post<ImportDetection>(`/imports/detect`, form);
    return validateRequest<ImportDetection>(response);
  }

  async function csvProbe(file: File): Promise<CsvProbeResponse> {
    const form = new FormData();
    form.append('file', file);
    const response = await api.post<CsvProbeResponse>(`/imports/csv/probe`, form);
    return validateRequest<CsvProbeResponse>(response);
  }

  async function preview(
    file: File,
    parserId: string,
    hints: ParseHints,
    maxRows: number = 50,
  ): Promise<ImportPreview> {
    const form = new FormData();
    form.append('file', file);
    form.append('parserId', parserId);
    form.append('hints', JSON.stringify(hints));
    form.append('maxRows', String(maxRows));
    const response = await api.post<ImportPreview>(`/imports/preview`, form);
    return validateRequest<ImportPreview>(response);
  }

  async function commit(
    accountId: string,
    file: File,
    parserId: string,
    hints: ParseHints,
  ): Promise<ImportResult> {
    const form = new FormData();
    form.append('file', file);
    form.append('parserId', parserId);
    form.append('hints', JSON.stringify(hints));
    const response = await api.post<ImportResult>(
      `/imports/${encodeURIComponent(accountId)}`, form,
    );
    return validateRequest<ImportResult>(response);
  }

  async function listProfiles(): Promise<CsvProfileSummary[]> {
    const response = await api.get<CsvProfileSummary[]>(`/imports/profiles`);
    return validateRequest<CsvProfileSummary[]>(response);
  }

  async function listParsers(): Promise<ParserSummary[]> {
    const response = await api.get<ParserSummary[]>(`/imports/parsers`);
    return validateRequest<ParserSummary[]>(response);
  }

  return {
    detect,
    csvProbe,
    preview,
    commit,
    listProfiles,
    listParsers,
  };
}
