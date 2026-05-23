import {defineStore} from "pinia";
import {useApi} from "~/composables/use-api";
import {useToasts} from "~/services/toasts/toast-service";
import {useApiErrors} from "~/composables/use-api-errors";
import {useIntegrationsService} from "~/services/integrations/integrations-service";
import type {ProviderDescriptor, SelectOption} from "~/models/integrations/provider-descriptor";
import type {ProviderConnection, ProviderConnectionForm} from "~/models/integrations/provider-connection";
import {upsertById} from "~/utils/upsert";

export const useProvidersStore = defineStore('providersStore', () => {
  const api = useApi();
  const toasts = useToasts();
  const apiErrors = useApiErrors();
  const integrationsService = useIntegrationsService(api);

  const descriptors = ref<ProviderDescriptor[]>([]);
  const connections = ref<ProviderConnection[]>([]);
  const pending = ref(false);

  const descriptorsByKey = computed(() =>
    new Map(descriptors.value.map(d => [d.key, d])),
  );

  const upsert = (connection: ProviderConnection) => upsertById(connections, connection);

  async function refresh() {
    pending.value = true;
    try {
      const fetchDescriptors = descriptors.value.length > 0
        ? Promise.resolve(descriptors.value)
        : integrationsService.fetchProviders();
      const [d, c] = await Promise.all([
        fetchDescriptors,
        integrationsService.fetchConnections(),
      ]);
      descriptors.value = d;
      connections.value = c;
    } catch (error) {
      apiErrors.toastError(error, "Failed to load integrations", "Could not load integrations");
    } finally {
      pending.value = false;
    }
  }

  async function createConnection(form: ProviderConnectionForm): Promise<ProviderConnection | undefined> {
    pending.value = true;
    try {
      const created = await integrationsService.createConnection(form);
      upsert(created);
      toasts.success("Connection created", `${created.displayName} is now connected`);
      return created;
    } catch (error) {
      apiErrors.toastError(error, "Failed to create connection", "Could not connect provider");
      throw error;
    } finally {
      pending.value = false;
    }
  }

  async function updateConnection(id: string, form: ProviderConnectionForm) {
    pending.value = true;
    try {
      upsert(await integrationsService.updateConnection(id, form));
      toasts.success("Connection updated", "Settings saved");
    } catch (error) {
      apiErrors.toastError(error, "Failed to update connection", "Could not save changes");
      throw error;
    } finally {
      pending.value = false;
    }
  }

  async function deleteConnection(id: string) {
    pending.value = true;
    try {
      await integrationsService.deleteConnection(id);
      connections.value = connections.value.filter(c => c.id !== id);
      toasts.success("Connection removed", "Provider has been disconnected");
    } catch (error) {
      apiErrors.toastError(error, "Failed to remove connection", "Could not disconnect");
      throw error;
    } finally {
      pending.value = false;
    }
  }

  async function triggerSync(id: string) {
    try {
      await integrationsService.triggerSync(id);
      toasts.success("Sync requested", "We'll fetch fresh data shortly");
    } catch (error) {
      apiErrors.toastError(error, "Failed to trigger sync", "Could not request sync");
    }
  }

  async function startOAuth(id: string): Promise<string | undefined> {
    try {
      const result = await integrationsService.startOAuth(id);
      return result.authorizationUrl;
    } catch (error) {
      apiErrors.toastError(error, "Failed to start authorization", "Could not begin OAuth flow");
      return undefined;
    }
  }

  async function searchProviderOptions(
    providerKey: string,
    fieldName: string,
    query: string,
    values: Record<string, unknown>,
  ): Promise<SelectOption[]> {
    try {
      return await integrationsService.searchProviderOptions(providerKey, fieldName, query, values);
    } catch (error) {
      apiErrors.toastError(error, "Search failed", "Could not load options");
      return [];
    }
  }

  function findDescriptor(key: string): ProviderDescriptor | undefined {
    return descriptorsByKey.value.get(key);
  }

  return {
    descriptors,
    descriptorsByKey,
    connections,
    pending,
    refresh,
    createConnection,
    updateConnection,
    deleteConnection,
    triggerSync,
    startOAuth,
    searchProviderOptions,
    findDescriptor,
  };
});
