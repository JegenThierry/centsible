<script lang="ts" setup>
import {computed, onMounted, onUnmounted, ref} from 'vue';
import {useNotificationsStore} from "~/stores/notificationsStore";
import type {NotificationType} from "~/models/notification/notification";
import FormattedDate from "~/components/_atoms/labels/formatted-date.vue";
import AppButton from "~/components/_atoms/ui/app-button.vue";

const store = useNotificationsStore();
const {t} = useI18n();
const open = ref(false);

const items = computed(() => store.notifications);

async function onOpen(value: boolean) {
  open.value = value;
  if (value) await store.refresh();
}

onMounted(() => {
  store.refreshCount();
  store.startPolling();
});

onUnmounted(() => {
  store.stopPolling();
});

const NOTIFICATION_STYLE: Record<NotificationType, {icon: string; color: string}> = {
  BUDGET_EXCEEDED: {icon: 'i-lucide-alert-circle', color: 'text-error'},
  BUDGET_THRESHOLD: {icon: 'i-lucide-bell-ring', color: 'text-warning'},
  BUDGET_PACE: {icon: 'i-lucide-trending-up', color: 'text-warning'},
  LOAN_DUE: {icon: 'i-lucide-hand-coins', color: 'text-warning'},
  RECURRING_UPCOMING: {icon: 'i-lucide-repeat', color: 'text-muted'},
  LARGE_TRANSACTION: {icon: 'i-lucide-arrow-up-right', color: 'text-info'},
  LOW_ACCOUNT_BALANCE: {icon: 'i-lucide-wallet', color: 'text-error'},
  PROJECTED_SHORTFALL: {icon: 'i-lucide-trending-down', color: 'text-warning'},
  SYNC_FAILED: {icon: 'i-lucide-refresh-cw-off', color: 'text-error'},
  CONSENT_EXPIRING: {icon: 'i-lucide-link-2-off', color: 'text-warning'},
};

function iconFor(type: NotificationType): string {
  return NOTIFICATION_STYLE[type]?.icon ?? 'i-lucide-bell-ring';
}

function colorFor(type: NotificationType): string {
  return NOTIFICATION_STYLE[type]?.color ?? 'text-warning';
}
</script>

<template>
  <UPopover :open="open" @update:open="onOpen">
    <AppButton :aria-label="t('notifications.aria')"
             color="neutral"
             icon="i-lucide-bell"
             variant="ghost">
      <template v-if="store.unreadCount > 0" #trailing>
        <span class="inline-flex items-center justify-center min-w-5 h-5 px-1 rounded-full bg-error text-white text-xs font-semibold">
          {{ store.unreadCount > 9 ? '9+' : store.unreadCount }}
        </span>
      </template>
    </AppButton>

    <template #content>
      <div class="w-96 max-w-[90vw] max-h-[70vh] flex flex-col">
        <div class="flex items-center justify-between p-3 border-b border-default">
          <h3 class="text-sm font-semibold">{{ t('notifications.title') }}</h3>
          <AppButton v-if="store.unreadCount > 0"
                   color="neutral"
                   size="xs"
                   variant="ghost"
                   @click="store.markAllRead">
            {{ t('notifications.markAllRead') }}
          </AppButton>
        </div>

        <div v-if="items.length === 0" class="p-8 text-center text-sm text-muted">
          {{ t('notifications.empty') }}
        </div>

        <div v-else class="flex-1 overflow-y-auto divide-y divide-default">
          <div v-for="n in items"
               :key="n.id"
               :class="['p-3 flex gap-3 group', !n.readAt && 'bg-primary-50/40 dark:bg-primary-900/10']">
            <UIcon :class="[colorFor(n.type), 'w-5 h-5 shrink-0 mt-0.5']" :name="iconFor(n.type)"/>
            <div class="flex-1 min-w-0">
              <div class="flex items-start justify-between gap-2">
                <p class="text-sm font-medium text-highlighted">{{ n.title }}</p>
                <span v-if="!n.readAt" class="w-2 h-2 rounded-full bg-primary-500 shrink-0 mt-1.5"/>
              </div>
              <p class="text-sm text-muted">{{ n.body }}</p>
              <p class="text-xs text-dimmed mt-1">
                <FormattedDate :date="n.createdAt" format="full"/>
              </p>
            </div>
            <div class="flex flex-col gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
              <AppButton v-if="!n.readAt"
                       :aria-label="t('notifications.markRead')"
                       color="neutral"
                       icon="i-lucide-check"
                       size="xs"
                       variant="ghost"
                       @click="store.markRead(n.id)"/>
              <AppButton :aria-label="t('notifications.delete')"
                       color="neutral"
                       icon="i-lucide-x"
                       size="xs"
                       variant="ghost"
                       @click="store.remove(n.id)"/>
            </div>
          </div>
        </div>
      </div>
    </template>
  </UPopover>
</template>
