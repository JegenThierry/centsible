import type {AxiosInstance} from "axios";
import {validateRequest} from "~/composables/use-api";
import type {Attachment, EnrichedAttachment} from "~/models/transactions/attachment";

export function useAttachmentService(api: AxiosInstance) {
  async function list(transactionId: string): Promise<Attachment[]> {
    const response = await api.get<Attachment[]>(
      `/transactions/${encodeURIComponent(transactionId)}/attachments`,
    );
    return validateRequest<Attachment[]>(response);
  }

  async function listForUser(page = 1, size = 25): Promise<EnrichedAttachment[]> {
    const response = await api.get<EnrichedAttachment[]>(`/attachments`, {params: {page, size}});
    return validateRequest<EnrichedAttachment[]>(response);
  }

  async function upload(transactionId: string, file: File): Promise<Attachment> {
    const form = new FormData();
    form.append('file', file);
    const response = await api.post<Attachment>(
      `/transactions/${encodeURIComponent(transactionId)}/attachments`,
      form,
      {headers: {'Content-Type': 'multipart/form-data'}},
    );
    return validateRequest<Attachment>(response);
  }

  async function remove(transactionId: string, attachmentId: string): Promise<void> {
    const response = await api.delete(
      `/transactions/${encodeURIComponent(transactionId)}/attachments/${encodeURIComponent(attachmentId)}`,
    );
    if (response.status !== 200 && response.status !== 204) {
      throw new Error(response.statusText);
    }
  }

  async function fetchBlob(transactionId: string, attachmentId: string): Promise<Blob> {
    const response = await api.get<Blob>(
      `/transactions/${encodeURIComponent(transactionId)}/attachments/${encodeURIComponent(attachmentId)}`,
      {responseType: 'blob'},
    );
    if (response.status !== 200) throw new Error(response.statusText);
    return response.data;
  }

  return {list, listForUser, upload, remove, fetchBlob};
}
