import type {AxiosInstance} from "axios";
import {validateRequest} from "~/composables/use-api";
import type {Tag, TagForm} from "~/models/tag/tag";

/** Thin wrapper around /api/tags and /api/transactions/{id}/tags. */
export function useTagService(api: AxiosInstance) {

  async function fetchAll(): Promise<Tag[]> {
    const response = await api.get<Tag[]>('/tags');
    return validateRequest<Tag[]>(response);
  }

  async function create(form: TagForm): Promise<Tag> {
    const response = await api.post<Tag>('/tags', form);
    return validateRequest<Tag>(response);
  }

  async function update(id: number, form: TagForm): Promise<Tag> {
    const response = await api.put<Tag>(`/tags/${id}`, form);
    return validateRequest<Tag>(response);
  }

  async function remove(id: number): Promise<void> {
    await api.delete(`/tags/${id}`);
  }

  async function fetchForTransaction(transactionId: string): Promise<Tag[]> {
    const response = await api.get<Tag[]>(`/transactions/${encodeURIComponent(transactionId)}/tags`);
    return validateRequest<Tag[]>(response);
  }

  async function setForTransaction(transactionId: string, tagIds: number[]): Promise<Tag[]> {
    const response = await api.put<Tag[]>(`/transactions/${encodeURIComponent(transactionId)}/tags`, {tagIds});
    return validateRequest<Tag[]>(response);
  }

  return {fetchAll, create, update, remove, fetchForTransaction, setForTransaction};
}
