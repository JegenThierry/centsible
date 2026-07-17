import {useNuxtApp} from "nuxt/app";
import type {AxiosInstance, AxiosResponse} from "axios";

export const useApi = () => {
  const nuxtApp = useNuxtApp()
  return nuxtApp.$axios;
}

export function validateRequest<T>(request: AxiosResponse<T>): T {
  if (request.status === 200 && request.data) {
    return request.data;
  }

  throw new Error(request.statusText);
}

/**
 * Throws unless the status is one of the accepted values. Use for endpoints that legitimately
 * return either 200 or 204 (or 202 for accepted-but-async) and don't carry a body we need.
 */
export function assertStatus(response: AxiosResponse, accepted: number[] = [200, 204]): void {
  if (!accepted.includes(response.status)) {
    throw new Error(response.statusText);
  }
}

/**
 * Standard REST CRUD client bound to a [basePath], returning `{list, get, create, update, remove}`
 * over the shared {@link validateRequest}/{@link assertStatus} helpers. Domain services wrap this and
 * re-export domain-named methods; services with quirks keep their bespoke methods and use this only
 * for their plain endpoints. [Id] defaults to `string`; numeric ids are URL-encoded via `String(id)`.
 */
export function crudResource<
  Entity,
  Id extends string | number = string,
  CreateForm = Entity,
  UpdateForm = CreateForm,
>(api: AxiosInstance, basePath: string) {
  const idPath = (id: Id) => `${basePath}/${encodeURIComponent(String(id))}`;

  async function list(): Promise<Entity[]> {
    return validateRequest<Entity[]>(await api.get<Entity[]>(basePath));
  }

  async function get(id: Id): Promise<Entity> {
    return validateRequest<Entity>(await api.get<Entity>(idPath(id)));
  }

  async function create(form: CreateForm): Promise<Entity> {
    return validateRequest<Entity>(await api.post<Entity>(basePath, form));
  }

  async function update(id: Id, form: UpdateForm): Promise<Entity> {
    return validateRequest<Entity>(await api.put<Entity>(idPath(id), form));
  }

  async function remove(id: Id): Promise<void> {
    assertStatus(await api.delete(idPath(id)));
  }

  return {list, get, create, update, remove};
}

export async function postMultipart<T>(
  api: AxiosInstance,
  url: string,
  fields: Record<string, string | Blob>,
): Promise<T> {
  const form = new FormData();
  for (const [key, value] of Object.entries(fields)) {
    form.append(key, value);
  }
  const response = await api.post<T>(url, form, {
    headers: {'Content-Type': 'multipart/form-data'},
  });
  return validateRequest<T>(response);
}
