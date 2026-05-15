import type {AxiosInstance} from "axios";
import {validateRequest} from "~/composables/use-api";
import type {ProviderDescriptor} from "~/models/integrations/provider-descriptor";
import type {ProviderConnection, ProviderConnectionForm} from "~/models/integrations/provider-connection";

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
    const response = await api.delete(`/integrations/connections/${encodeURIComponent(id)}`);
    if (response.status !== 200 && response.status !== 204) {
      throw new Error(response.statusText);
    }
  }

  async function triggerSync(id: string): Promise<void> {
    const response = await api.post(`/integrations/connections/${encodeURIComponent(id)}/sync`);
    if (response.status !== 200 && response.status !== 202) {
      throw new Error(response.statusText);
    }
  }

  return {
    fetchProviders,
    fetchConnections,
    createConnection,
    updateConnection,
    deleteConnection,
    triggerSync,
  };
}
