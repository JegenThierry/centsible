import type {AxiosInstance} from "axios";
import {assertStatus, validateRequest} from "~/composables/use-api";
import type {ProviderDescriptor, SelectOption} from "~/models/integrations/provider-descriptor";
import type {ProviderConnection, ProviderConnectionForm} from "~/models/integrations/provider-connection";

export interface OAuthStartResponse {
  authorizationUrl: string;
  connectionId: string;
}

export function useIntegrationsService(api: AxiosInstance) {
  async function fetchProviders(): Promise<ProviderDescriptor[]> {
    const response = await api.get<ProviderDescriptor[]>('/integrations/providers');
    return validateRequest<ProviderDescriptor[]>(response);
  }

  async function fetchConnections(): Promise<ProviderConnection[]> {
    const response = await api.get<ProviderConnection[]>('/integrations/connections');
    return validateRequest<ProviderConnection[]>(response);
  }

  async function createConnection(form: ProviderConnectionForm): Promise<ProviderConnection> {
    const response = await api.post<ProviderConnection>('/integrations/connections', form);
    return validateRequest<ProviderConnection>(response);
  }

  async function updateConnection(id: string, form: ProviderConnectionForm): Promise<ProviderConnection> {
    const response = await api.put<ProviderConnection>(
      `/integrations/connections/${encodeURIComponent(id)}`,
      form,
    );
    return validateRequest<ProviderConnection>(response);
  }

  async function deleteConnection(id: string): Promise<void> {
    assertStatus(await api.delete(`/integrations/connections/${encodeURIComponent(id)}`));
  }

  async function triggerSync(id: string): Promise<void> {
    assertStatus(
      await api.post(`/integrations/connections/${encodeURIComponent(id)}/sync`),
      [200, 202],
    );
  }

  async function startOAuth(id: string): Promise<OAuthStartResponse> {
    const response = await api.post<OAuthStartResponse>(
      `/integrations/connections/${encodeURIComponent(id)}/oauth/start`,
    );
    return validateRequest<OAuthStartResponse>(response);
  }

  async function searchProviderOptions(
    providerKey: string,
    fieldName: string,
    query: string,
    values: Record<string, unknown>,
  ): Promise<SelectOption[]> {
    const response = await api.post<SelectOption[]>(
      `/integrations/providers/${encodeURIComponent(providerKey)}/options/${encodeURIComponent(fieldName)}`,
      {query, values},
    );
    return validateRequest<SelectOption[]>(response);
  }

  return {
    fetchProviders,
    fetchConnections,
    createConnection,
    updateConnection,
    deleteConnection,
    triggerSync,
    startOAuth,
    searchProviderOptions,
  };
}
