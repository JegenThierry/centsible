<script lang="ts" setup>
import AppButton from "~/components/_atoms/ui/app-button.vue";
import ConfirmationModal from "~/components/_organisms/modals/confirmation-modal.vue";
import {useAuthStore} from "~/stores/authStore";
import {useToasts} from "~/services/toasts/toast-service";

const {t} = useI18n();
const authStore = useAuthStore();
const {success, error} = useToasts();

const confirmOpen = ref(false);

async function confirmSignOut() {
  try {
    await authStore.signOutEverywhere();
    success(t('profile.sessions.toasts.successTitle'), t('profile.sessions.toasts.successBody'));
  } catch (e) {
    error(t('profile.sessions.toasts.errorTitle'), t('profile.sessions.toasts.errorBody'));
    throw e;
  }
}
</script>

<template>
  <div>
    <h3 class="text-lg font-semibold">{{ t('profile.sessions.title') }}</h3>
    <p class="text-sm text-muted mt-1">{{ t('profile.sessions.description') }}</p>

    <div class="mt-4 flex">
      <AppButton color="neutral" variant="subtle" icon="i-lucide-log-out" @click="confirmOpen = true">
        {{ t('profile.sessions.signOutEverywhere') }}
      </AppButton>
    </div>

    <ConfirmationModal v-model:open="confirmOpen"
                       :title="t('profile.sessions.confirm.title')"
                       :body="t('profile.sessions.confirm.body')"
                       :confirm-label="t('profile.sessions.signOutEverywhere')"
                       confirm-color="warning"
                       :manage-toasts="false"
                       :delete-callback="confirmSignOut"/>
  </div>
</template>
