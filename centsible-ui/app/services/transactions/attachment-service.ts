import type {AxiosInstance} from "axios";
import {assertStatus, postMultipart, validateRequest} from "~/composables/use-api";
import type {Attachment, EnrichedAttachment} from "~/models/transactions/attachment";

export function useAttachmentService(api: AxiosInstance) {
  async function list(transactionId: string): Promise<Attachment[]> {
    const response = await api.get<Attachment[]>(
      `/transactions/${encodeURIComponent(transactionId)}/attachments`,
    );
    return validateRequest<Attachment[]>(response);
  }

  /** Lists every attachment across all the user's transactions, paged. */
  async function listForUser(page = 1, size = 25): Promise<EnrichedAttachment[]> {
    const response = await api.get<EnrichedAttachment[]>(`/attachments`, {params: {page, size}});
    return validateRequest<EnrichedAttachment[]>(response);
  }

  async function upload(transactionId: string, file: File): Promise<Attachment> {
    return postMultipart<Attachment>(
      api,
      `/transactions/${encodeURIComponent(transactionId)}/attachments`,
      {file},
    );
  }

  async function remove(transactionId: string, attachmentId: string): Promise<void> {
    assertStatus(await api.delete(
      `/transactions/${encodeURIComponent(transactionId)}/attachments/${encodeURIComponent(attachmentId)}`,
    ));
  }

  async function fetchBlob(transactionId: string, attachmentId: string): Promise<Blob> {
    const response = await api.get<Blob>(
      `/transactions/${encodeURIComponent(transactionId)}/attachments/${encodeURIComponent(attachmentId)}`,
      {responseType: 'blob'},
    );
    assertStatus(response, [200]);
    return response.data;
  }

  return {list, listForUser, upload, remove, fetchBlob};
}
