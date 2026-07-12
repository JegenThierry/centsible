<script lang="ts" setup>
import BaseInput from "~/components/_atoms/inputs/base-input.vue";
import PasswordInput from "~/components/_atoms/inputs/password-input.vue";
import ModalFooterActions from "~/components/_molecules/modals/modal-footer-actions.vue";
import {useAuthStore} from "~/stores/authStore";
import {useUserStore} from "~/stores/userStore";
import {useTotpService} from "~/services/auth/totp-service";
import {useToasts} from "~/services/toasts/toast-service";
import {useApi} from "~/composables/use-api";

const {t} = useI18n();
const isOpen = defineModel<boolean>('open', {required: true});
const authStore = useAuthStore();
const userStore = useUserStore();
const totpService = useTotpService(useApi());
const {success, error} = useToasts();

const password = ref('');
const totpCode = ref('');
const confirmText = ref('');
const twoFactorEnabled = ref(false);
const loading = ref(false);

const username = computed(() => userStore.user?.username ?? '');

watch(isOpen, async (open) => {
  if (!open) return;
  password.value = '';
  totpCode.value = '';
  confirmText.value = '';
  try {
    twoFactorEnabled.value = (await totpService.status()).enabled;
  } catch {
    twoFactorEnabled.value = false;
  }
});

const confirmMatches = computed(() => confirmText.value.trim() === username.value && username.value.length > 0);

const canSubmit = computed(() =>
  password.value.length > 0
  && confirmMatches.value
  && (!twoFactorEnabled.value || totpCode.value.trim().length > 0),
);

async function onConfirm() {
  if (!canSubmit.value || loading.value) return;
  loading.value = true;
  try {
    const code = twoFactorEnabled.value ? totpCode.value.trim() : undefined;
    await authStore.deleteAccount(password.value, code);
    success(t('profile.dangerZone.delete.toasts.successTitle'), t('profile.dangerZone.delete.toasts.successBody'));
  } catch (e: any) {
    if (e?.response?.status === 401) {
      error(t('profile.dangerZone.delete.toasts.passwordIncorrectTitle'), t('profile.dangerZone.delete.toasts.passwordIncorrectBody'));
    } else {
      error(t('profile.dangerZone.delete.toasts.errorTitle'), t('profile.dangerZone.delete.toasts.errorBody'));
    }
    loading.value = false;
  }
}
</script>

<template>
  <UModal v-model:open="isOpen" :title="t('profile.dangerZone.delete.confirmTitle')">
    <template #body>
      <p class="text-sm">{{ t('profile.dangerZone.delete.confirmBody') }}</p>
      <div class="mt-4 space-y-4">
        <PasswordInput v-model="password"
                       required
                       :label="t('profile.dangerZone.delete.passwordLabel')"
                       :placeholder="t('profile.dangerZone.delete.passwordPlaceholder')"
                       :description="t('profile.dangerZone.delete.passwordHint')"/>
        <BaseInput v-if="twoFactorEnabled"
                   v-model="totpCode"
                   type="text"
                   :label="t('profile.dangerZone.delete.totpLabel')"
                   :hint="t('profile.dangerZone.delete.totpHint')"/>
        <BaseInput v-model="confirmText"
                   type="text"
                   :label="t('profile.dangerZone.delete.confirmTextLabel')"
                   :hint="t('profile.dangerZone.delete.confirmTextHint', {username})"/>
      </div>
    </template>

    <template #footer>
      <ModalFooterActions :loading="loading"
                          :disabled="!canSubmit"
                          :submit-label="t('profile.dangerZone.delete.confirmButton')"
                          submit-color="error"
                          @cancel="isOpen = false"
                          @submit="onConfirm"/>
    </template>
  </UModal>
</template>
