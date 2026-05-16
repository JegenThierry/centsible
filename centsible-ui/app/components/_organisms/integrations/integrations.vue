<script lang="ts" setup>
import {useProvidersStore} from "~/stores/providersStore";
import type {ProviderDescriptor} from "~/models/integrations/provider-descriptor";
import type {ProviderConnection} from "~/models/integrations/provider-connection";
import PageHeader from "~/components/_molecules/page/page-header.vue";
import AppEmptyState from "~/components/_molecules/feedback/app-empty-state.vue";
import LoadingAnimation from "~/components/_atoms/animations/loading-animation.vue";
import ConnectProviderModal from "~/components/_organisms/integrations/modals/connect-provider-modal.vue";

const providersStore = useProvidersStore();
const {t} = useI18n();
const localeTag = useLocaleTag();

const isConnectOpen = ref(false);
const activeDescriptor = ref<ProviderDescriptor | undefined>();

function openConnect(descriptor: ProviderDescriptor) {
  activeDescriptor.value = descriptor;
  isConnectOpen.value = true;
}

function statusColor(status: ProviderConnection['status']): 'success' | 'warning' | 'error' | 'neutral' {
  switch (status) {
    case 'ACTIVE': return 'success';
    case 'NEW': return 'warning';
    case 'ERROR': return 'error';
    case 'REVOKED': return 'neutral';
  }
}

function statusLabel(status: ProviderConnection['status']): string {
  return t(`integrations.connections.status.${status}`);
}

function formatLastSync(value: string): string {
  return new Date(value).toLocaleString(localeTag.value);
}

function descriptorFor(connection: ProviderConnection): ProviderDescriptor | undefined {
  return providersStore.findDescriptor(connection.providerKey);
}

async function onSync(connection: ProviderConnection) {
  await providersStore.triggerSync(connection.id);
}

async function onDelete(connection: ProviderConnection) {
  if (!confirm(t('integrations.connections.confirmDisconnect', {name: connection.displayName}))) return;
  await providersStore.deleteConnection(connection.id);
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
        <UCard v-for="d in providersStore.descriptors" :key="d.key">
          <div class="flex flex-col gap-3 h-full">
            <div>
              <div class="flex items-center gap-2">
                <UIcon class="w-5 h-5" name="i-lucide-plug"/>
                <h3 class="font-semibold">{{ d.displayName }}</h3>
              </div>
              <p v-if="d.description" class="text-sm text-neutral-500 mt-1">{{ d.description }}</p>
            </div>
            <div v-if="d.capabilities.length > 0" class="flex flex-wrap gap-1">
              <UBadge v-for="c in d.capabilities" :key="c" color="neutral" size="sm" variant="soft">
                {{ c.toLowerCase() }}
              </UBadge>
            </div>
            <div class="flex-1"/>
            <UButton block icon="i-lucide-plus" @click="openConnect(d)">
              {{ t('integrations.available.connect') }}
            </UButton>
          </div>
        </UCard>
      </div>
    </section>

    <section>
      <h2 class="text-lg font-semibold mb-3">{{ t('integrations.connections.heading') }}</h2>
      <AppEmptyState v-if="!providersStore.pending && providersStore.connections.length === 0"
                     :description="t('integrations.connections.emptyDescription')"
                     icon="i-lucide-cable"
                     :title="t('integrations.connections.emptyTitle')"/>
      <div v-else class="space-y-3">
        <UCard v-for="conn in providersStore.connections" :key="conn.id">
          <div class="flex items-start justify-between gap-4 flex-wrap">
            <div class="flex-1 min-w-0">
              <div class="flex items-center gap-2">
                <h3 class="font-semibold truncate">{{ conn.displayName }}</h3>
                <UBadge :color="statusColor(conn.status)" size="sm" variant="soft">
                  {{ statusLabel(conn.status) }}
                </UBadge>
              </div>
              <p class="text-sm text-neutral-500">
                {{ descriptorFor(conn)?.displayName ?? conn.providerKey }}
              </p>
              <p v-if="conn.lastSyncAt" class="text-xs text-dimmed mt-1">
                {{ t('integrations.connections.lastSynced', {date: formatLastSync(conn.lastSyncAt)}) }}
              </p>
              <p v-if="conn.lastError" class="text-xs text-error mt-1 truncate" :title="conn.lastError">
                {{ t('integrations.connections.lastError', {error: conn.lastError}) }}
              </p>
            </div>
            <div class="flex gap-2">
              <UButton color="neutral"
                       icon="i-lucide-refresh-cw"
                       size="sm"
                       variant="soft"
                       @click="onSync(conn)">
                {{ t('integrations.connections.syncNow') }}
              </UButton>
              <UButton color="error"
                       icon="i-lucide-trash-2"
                       size="sm"
                       variant="soft"
                       @click="onDelete(conn)">
                {{ t('integrations.connections.disconnect') }}
              </UButton>
            </div>
          </div>
        </UCard>
      </div>
    </section>

    <ConnectProviderModal v-model:open="isConnectOpen" :descriptor="activeDescriptor"/>
  </UContainer>
</template>
