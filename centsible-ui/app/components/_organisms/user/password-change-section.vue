<script lang="ts" setup>
import PasswordInput from "~/components/_atoms/inputs/password-input.vue";
import AppButton from "~/components/_atoms/ui/app-button.vue";
import {useAuthStore} from "~/stores/authStore";
import {useToasts} from "~/services/toasts/toast-service";
import {useValidator} from "~/composables/use-validator";

const {t} = useI18n();
const authStore = useAuthStore();
const {success, error} = useToasts();
const {validateInputs} = useValidator();

// Mirrors the server-side strength rule so we can fail fast before the round-trip.
const PASSWORD_PATTERN = /^(?=.*[A-Z])(?=.*[a-z])(?=.*\d)(?=.*[@$!%*?&]).{8,}$/;

const state = reactive({currentPassword: '', newPassword: '', confirmPassword: ''});
const loading = ref(false);

const currentInput = ref<InstanceType<typeof PasswordInput>>();
const newInput = ref<InstanceType<typeof PasswordInput>>();
const confirmInput = ref<InstanceType<typeof PasswordInput>>();

const isStrong = () => PASSWORD_PATTERN.test(state.newPassword);
const differsFromCurrent = () => state.newPassword !== state.currentPassword;
const matchesConfirm = () => state.newPassword === state.confirmPassword;

const newPasswordMessage = computed(() =>
  !isStrong()
    ? t('profile.password.validation.tooWeak')
    : t('profile.password.validation.mustDiffer'),
);

function reset() {
  state.currentPassword = '';
  state.newPassword = '';
  state.confirmPassword = '';
}

async function onSubmit() {
  if (!validateInputs([currentInput, newInput, confirmInput])) return;
  loading.value = true;
  try {
    await authStore.changePassword(state.currentPassword, state.newPassword);
    success(t('profile.password.toasts.successTitle'), t('profile.password.toasts.successBody'));
    reset();
  } catch (e: any) {
    const status = e?.response?.status;
    if (status === 401) {
      error(t('profile.password.toasts.currentIncorrectTitle'), t('profile.password.toasts.currentIncorrectBody'));
    } else {
      error(t('profile.password.toasts.errorTitle'), t('profile.password.toasts.errorBody'));
    }
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <div>
    <h3 class="text-lg font-semibold">{{ t('profile.password.title') }}</h3>
    <p class="text-sm text-muted mt-1">{{ t('profile.password.description') }}</p>

    <UForm :state="state" class="mt-4 space-y-4" @submit="onSubmit">
      <PasswordInput ref="currentInput"
                     v-model="state.currentPassword"
                     required
                     :label="t('profile.password.currentLabel')"
                     :placeholder="t('profile.password.currentPlaceholder')"/>
      <PasswordInput ref="newInput"
                     v-model="state.newPassword"
                     required
                     :label="t('profile.password.newLabel')"
                     :placeholder="t('profile.password.newPlaceholder')"
                     :description="t('profile.password.newHint')"
                     :additional-validation="() => isStrong() && differsFromCurrent()"
                     :additional-validation-message="newPasswordMessage"/>
      <PasswordInput ref="confirmInput"
                     v-model="state.confirmPassword"
                     required
                     :label="t('profile.password.confirmLabel')"
                     :placeholder="t('profile.password.confirmPlaceholder')"
                     :additional-validation="matchesConfirm"
                     :additional-validation-message="t('profile.password.validation.mustMatch')"/>
      <div class="flex">
        <AppButton class="ml-auto" type="submit" :loading="loading">
          {{ t('profile.password.submit') }}
        </AppButton>
      </div>
    </UForm>
  </div>
</template>
