<script lang="ts" setup>
import adze from 'adze'
import AppSwitch from "~/components/_atoms/ui/app-switch.vue";
import AppInput from "~/components/_atoms/ui/app-input.vue";
import AppButton from "~/components/_atoms/ui/app-button.vue";
import type {NotificationSettings} from "~/models/notification/notification-settings";
import {DEFAULT_NOTIFICATION_SETTINGS} from "~/models/notification/notification-settings";
import {useUserService} from "~/services/user/user-service";
import {useToasts} from "~/services/toasts/toast-service";

const service = useUserService(useApi());
const toasts = useToasts();
const {t} = useI18n();

const settings = ref<NotificationSettings>({...DEFAULT_NOTIFICATION_SETTINGS});
const enableLargeTxn = ref(false);
const enableLowBalance = ref(false);
const loading = ref(false);
const saving = ref(false);

async function load() {
  loading.value = true;
  try {
    const fetched = await service.fetchNotificationSettings();
    settings.value = {...DEFAULT_NOTIFICATION_SETTINGS, ...fetched};
    enableLargeTxn.value = settings.value.largeTransactionThreshold !== null;
    enableLowBalance.value = settings.value.lowBalanceThreshold !== null;
  } catch (error) {
    adze.ns('notifications').error('Failed to load notification settings', error);
  } finally {
    loading.value = false;
  }
}

async function save() {
  saving.value = true;
  try {
    const payload: NotificationSettings = {
      ...settings.value,
      // The two threshold fields carry real toggle logic (null = disabled); everything else
      // (days-ahead ints, budgetAlertsEnabled, and any future plain field) rides the spread.
      largeTransactionThreshold: enableLargeTxn.value ? Number(settings.value.largeTransactionThreshold ?? 0) : null,
      lowBalanceThreshold: enableLowBalance.value ? Number(settings.value.lowBalanceThreshold ?? 0) : null,
      loanDueDaysAhead: Number(settings.value.loanDueDaysAhead ?? 0),
      recurringDueDaysAhead: Number(settings.value.recurringDueDaysAhead ?? 0),
    };
    await service.updateNotificationSettings(payload);
    toasts.success(t('profile.notifications.toasts.savedTitle'), t('profile.notifications.toasts.savedBody'));
  } catch (error) {
    adze.ns('notifications').error('Failed to save notification settings', error);
    toasts.error(t('profile.notifications.toasts.errorTitle'), t('profile.notifications.toasts.errorBody'));
  } finally {
    saving.value = false;
  }
}

onMounted(() => load());
</script>

<template>
  <div class="space-y-4">
    <div>
      <h3 class="text-lg font-semibold">{{ t('profile.notifications.title') }}</h3>
      <p class="text-sm text-muted mt-1">{{ t('profile.notifications.description') }}</p>
    </div>

    <div class="space-y-4">
      <div class="flex items-start justify-between gap-3">
        <div class="min-w-0">
          <p class="font-medium text-sm">{{ t('profile.notifications.largeTransaction.title') }}</p>
          <p class="text-xs text-muted">{{ t('profile.notifications.largeTransaction.description') }}</p>
        </div>
        <AppSwitch v-model="enableLargeTxn" :disabled="loading"/>
      </div>
      <AppInput v-if="enableLargeTxn"
              v-model.number="settings.largeTransactionThreshold"
              :min="0"
              :placeholder="t('profile.notifications.largeTransaction.placeholder')"
              :disabled="loading"
              class="w-40"
              type="number"/>

      <div class="flex items-start justify-between gap-3 pt-2 border-t border-default">
        <div class="min-w-0">
          <p class="font-medium text-sm">{{ t('profile.notifications.lowBalance.title') }}</p>
          <p class="text-xs text-muted">{{ t('profile.notifications.lowBalance.description') }}</p>
        </div>
        <AppSwitch v-model="enableLowBalance" :disabled="loading"/>
      </div>
      <AppInput v-if="enableLowBalance"
              v-model.number="settings.lowBalanceThreshold"
              :min="0"
              :placeholder="t('profile.notifications.lowBalance.placeholder')"
              :disabled="loading"
              class="w-40"
              type="number"/>

      <div class="flex items-start justify-between gap-3 pt-2 border-t border-default">
        <div class="min-w-0">
          <p class="font-medium text-sm">{{ t('profile.notifications.budgetAlerts.title') }}</p>
          <p class="text-xs text-muted">{{ t('profile.notifications.budgetAlerts.description') }}</p>
        </div>
        <AppSwitch v-model="settings.budgetAlertsEnabled" :disabled="loading"/>
      </div>

      <div class="grid grid-cols-2 gap-3 pt-2 border-t border-default">
        <UFormField :label="t('profile.notifications.loanDueDaysAhead')">
          <AppInput v-model.number="settings.loanDueDaysAhead"
                  :min="0"
                  :max="30"
                  :disabled="loading"
                  type="number"/>
        </UFormField>
        <UFormField :label="t('profile.notifications.recurringDueDaysAhead')">
          <AppInput v-model.number="settings.recurringDueDaysAhead"
                  :min="0"
                  :max="30"
                  :disabled="loading"
                  type="number"/>
        </UFormField>
      </div>

      <div class="flex justify-end pt-2">
        <AppButton :loading="saving" @click="save">{{ t('profile.notifications.save') }}</AppButton>
      </div>
    </div>
  </div>
</template>
