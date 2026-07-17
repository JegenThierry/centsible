<script lang="ts" setup>
import {useProvidersStore} from "~/stores/providersStore";
import type {ProviderDescriptor} from "~/models/integrations/provider-descriptor";
import type {ProviderConnection} from "~/models/integrations/provider-connection";
import PageHeader from "~/components/_molecules/page/page-header.vue";
import AppEmptyState from "~/components/_molecules/feedback/app-empty-state.vue";
import LoadingAnimation from "~/components/_atoms/animations/loading-animation.vue";
import FormattedDate from "~/components/_atoms/labels/formatted-date.vue";
import IntegrationCard, {type IntegrationCardBadge} from "~/components/_molecules/integrations/integration-card.vue";
import AppButton from "~/components/_atoms/ui/app-button.vue";
const ConnectProviderModal = defineAsyncComponent(() => import("~/components/_organisms/integrations/modals/connect-provider-modal.vue"));
const ConfirmationModal = defineAsyncComponent(() => import("~/components/_organisms/modals/confirmation-modal.vue"));

const providersStore = useProvidersStore();
const {t} = useI18n();

const isConnectOpen = ref(false);
const activeDescriptor = ref<ProviderDescriptor | undefined>();
const isConfirmDeleteOpen = ref(false);
const pendingDelete = ref<ProviderConnection | undefined>();

function openConnect(descriptor: ProviderDescriptor) {
  activeDescriptor.value = descriptor;
  isConnectOpen.value = true;
}

const STATUS_COLOR: Record<ProviderConnection['status'], 'success' | 'warning' | 'error' | 'neutral'> = {
  ACTIVE: 'success',
  NEW: 'warning',
  ERROR: 'error',
  REVOKED: 'neutral',
};

function statusColor(status: ProviderConnection['status']) {
  return STATUS_COLOR[status];
}

function statusLabel(status: ProviderConnection['status']): string {
  return t(`integrations.connections.status.${status}`);
}

function descriptorFor(connection: ProviderConnection): ProviderDescriptor | undefined {
  return providersStore.findDescriptor(connection.providerKey);
}

function descriptorBadges(d: ProviderDescriptor): IntegrationCardBadge[] {
  return d.capabilities.map(c => ({label: c.toLowerCase()}));
}

function connectionBadges(conn: ProviderConnection): IntegrationCardBadge[] {
  return [{label: statusLabel(conn.status), color: statusColor(conn.status)}];
}

async function onSync(connection: ProviderConnection) {
  await providersStore.triggerSync(connection.id);
}

function onDelete(connection: ProviderConnection) {
  pendingDelete.value = connection;
  isConfirmDeleteOpen.value = true;
}

async function confirmDelete() {
  const target = pendingDelete.value;
  if (!target) return;
  try {
    await providersStore.deleteConnection(target.id);
  } catch {
    // The store already toasted the failure; swallow so the click handler doesn't reject.
  } finally {
    pendingDelete.value = undefined;
  }
}

async function onReconnect(connection: ProviderConnection) {
  const url = await providersStore.startOAuth(connection.id);
  if (url) window.location.href = url;
}

onMounted(() => {
  providersStore.refresh();
});
</script>

<template>
  <UContainer class="py-6 sm:py-10">
    <PageHeader :description="t('integrations.page.description')"
                :title="t('integrations.page.title')"/>

    <div v-if="providersStore.pending && providersStore.descriptors.length === 0"
         class="flex justify-center my-10">
      <LoadingAnimation/>
    </div>

    <section class="mb-10">
      <h2 class="text-lg font-semibold mb-3">{{ t('integrations.available.heading') }}</h2>
      <AppEmptyState v-if="!providersStore.pending && providersStore.descriptors.length === 0"
                     :description="t('integrations.available.emptyDescription')"
                     icon="i-lucide-plug-zap"
                     :title="t('integrations.available.emptyTitle')"/>
      <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        <IntegrationCard v-for="d in providersStore.descriptors"
                         :key="d.key"
                         :badges="descriptorBadges(d)"
                         :description="d.description"
                         icon="i-lucide-plug"
                         :title="d.displayName">
          <template #footer>
            <AppButton icon="i-lucide-plus" @click="openConnect(d)">
              {{ t('integrations.available.connect') }}
            </AppButton>
          </template>
        </IntegrationCard>
      </div>
    </section>

    <section>
      <h2 class="text-lg font-semibold mb-3">{{ t('integrations.connections.heading') }}</h2>
      <AppEmptyState v-if="!providersStore.pending && providersStore.connections.length === 0"
                     :description="t('integrations.connections.emptyDescription')"
                     icon="i-lucide-cable"
                     :title="t('integrations.connections.emptyTitle')"/>
      <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        <IntegrationCard v-for="conn in providersStore.connections"
                         :key="conn.id"
                         :badges="connectionBadges(conn)"
                         :description="descriptorFor(conn)?.displayName ?? conn.providerKey"
                         icon="i-lucide-cable"
                         :title="conn.displayName">
          <p v-if="conn.lastSyncAt" class="text-xs text-dimmed flex gap-1">
            <span>{{ t('integrations.connections.lastSyncedLabel') }}</span>
            <FormattedDate :date="conn.lastSyncAt"/>
          </p>
          <p v-if="conn.lastError" class="text-xs text-error truncate" :title="conn.lastError">
            {{ t('integrations.connections.lastError', {error: conn.lastError}) }}
          </p>

          <template #footer>
            <AppButton v-if="conn.status === 'REVOKED' || conn.status === 'NEW' || conn.status === 'ERROR'"
                     color="primary"
                     icon="i-lucide-link"
                     @click="onReconnect(conn)">
              {{ t('integrations.connections.reconnect') }}
            </AppButton>
            <AppButton color="neutral"
                     icon="i-lucide-refresh-cw"
                     variant="soft"
                     @click="onSync(conn)">
              {{ t('integrations.connections.syncNow') }}
            </AppButton>
            <AppButton color="error"
                     icon="i-lucide-trash-2"
                     variant="soft"
                     @click="onDelete(conn)">
              {{ t('integrations.connections.disconnect') }}
            </AppButton>
          </template>
        </IntegrationCard>
      </div>
    </section>

    <ConnectProviderModal v-model:open="isConnectOpen" :descriptor="activeDescriptor"/>
    <ConfirmationModal v-if="pendingDelete"
                       v-model:open="isConfirmDeleteOpen"
                       :delete-callback="confirmDelete"
                       :entity="pendingDelete.displayName"/>
  </UContainer>
</template>
