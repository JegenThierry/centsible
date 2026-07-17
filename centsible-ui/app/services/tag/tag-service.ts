import type {AxiosInstance} from "axios";
import {crudResource, validateRequest} from "~/composables/use-api";
import type {Tag, TagForm} from "~/models/tag/tag";

/** Thin wrapper around /api/tags and /api/transactions/{id}/tags. */
export function useTagService(api: AxiosInstance) {
  const resource = crudResource<Tag, number, TagForm>(api, '/tags');

  async function fetchForTransaction(transactionId: string): Promise<Tag[]> {
    const response = await api.get<Tag[]>(`/transactions/${encodeURIComponent(transactionId)}/tags`);
    return validateRequest<Tag[]>(response);
  }

  async function setForTransaction(transactionId: string, tagIds: number[]): Promise<Tag[]> {
    const response = await api.put<Tag[]>(`/transactions/${encodeURIComponent(transactionId)}/tags`, {tagIds});
    return validateRequest<Tag[]>(response);
  }

  return {
    fetchAll: (): Promise<Tag[]> => resource.list(),
    create: (form: TagForm): Promise<Tag> => resource.create(form),
    update: (id: number, form: TagForm): Promise<Tag> => resource.update(id, form),
    remove: (id: number): Promise<void> => resource.remove(id),
    fetchForTransaction,
    setForTransaction,
  };
}
