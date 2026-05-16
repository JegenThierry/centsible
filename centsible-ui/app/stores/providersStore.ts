import {defineStore} from "pinia";
import {useApi} from "~/composables/use-api";
import {useToasts} from "~/services/toasts/toast-service";
import {useApiErrors} from "~/composables/use-api-errors";
import {useIntegrationsService} from "~/services/integrations/integrations-service";
import type {ProviderDescriptor} from "~/models/integrations/provider-descriptor";
import type {ProviderConnection, ProviderConnectionForm} from "~/models/integrations/provider-connection";

export const useProvidersStore = defineStore('providersStore', () => {
  const api = useApi();
  const toasts = useToasts();
  const integrationsService = useIntegrationsService(api);

  const descriptors = ref<ProviderDescriptor[]>([]);
  const connections = ref<ProviderConnection[]>([]);
  const pending = ref(false);

  // Descriptors only change on deploy — cache O(1) by key for per-row lookups in the UI.
  const descriptorsByKey = computed(() =>
    new Map(descriptors.value.map(d => [d.key, d])),
  );

  function upsert(connection: ProviderConnection) {
    const idx = connections.value.findIndex(c => c.id === connection.id);
    if (idx >= 0) connections.value[idx] = connection;
    else connections.value = [connection, ...connections.value];
  }

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
      useApiErrors().toastError(error, "Failed to load integrations", "Could not load integrations");
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
      useApiErrors().toastError(error, "Failed to create connection", "Could not connect provider");
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
      useApiErrors().toastError(error, "Failed to update connection", "Could not save changes");
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
      useApiErrors().toastError(error, "Failed to remove connection", "Could not disconnect");
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
      useApiErrors().toastError(error, "Failed to trigger sync", "Could not request sync");
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
    findDescriptor,
  };
});
