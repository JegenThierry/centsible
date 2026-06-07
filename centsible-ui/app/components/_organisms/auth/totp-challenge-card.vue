<script lang="ts" setup>
import adze from 'adze'
import {useApi} from "~/composables/use-api";
import {useAuthService} from "~/services/auth/auth-service";
import {useToasts} from "~/services/toasts/toast-service";
import BaseInput from "~/components/_atoms/inputs/base-input.vue";
import AppButton from "~/components/_atoms/ui/app-button.vue";
import {useValidator} from "~/composables/use-validator";

const {t} = useI18n();
const api = useApi();
const authStore = useAuthStore();
const userStore = useUserStore();
const {success, error} = useToasts();

const state = reactive({
  code: '',
});
const loading = ref<boolean>(false);
const codeInput = ref<InstanceType<typeof BaseInput>>();

function validate(): boolean {
  return useValidator().validateInputs([codeInput]);
}

function onSubmit() {
  if (loading.value) return;
  if (!validate()) return;
  loading.value = true;
  useAuthService(api)
    .twoFactorChallenge(state.code.trim())
    .then(async () => {
      authStore.setTwoFactorPending(false);
      authStore.setAuthenticated(true);
      await userStore.fetchMyself();
      navigateTo('/accounts');
      success(t('auth.login.toastSuccessTitle'), t('auth.login.toastSuccessBody'));
    })
    .catch((err) => {
      adze.ns('auth').warn('2FA challenge failed', err);
      state.code = '';
      error(t('auth.twoFactor.challenge.toastErrorTitle'), t('auth.twoFactor.challenge.toastErrorBody'));
    })
    .finally(() => loading.value = false);
}
</script>

<template>
  <UContainer class="py-12">
    <UPageCard
      :description="t('auth.twoFactor.challenge.description')"
      :title="t('auth.twoFactor.challenge.title')"
      icon="i-lucide-shield-check"
      :ui="{ leadingIcon: 'text-primary' }"
      class="max-w-xl mx-auto"
      spotlight
      spotlight-color="primary">
      <UForm :state="state" class="space-y-6 pt-4 flex flex-col" @submit="onSubmit">
        <BaseInput ref="codeInput"
                   v-model="state.code"
                   autofocus
                   :disabled="loading"
                   :label="t('auth.twoFactor.challenge.codeLabel')"
                   :hint="t('auth.twoFactor.challenge.codeHint')"
                   required
                   type="text"/>

        <AppButton :loading="loading" class="ml-auto" type="submit">
          {{ t('auth.twoFactor.challenge.submit') }}
        </AppButton>
      </UForm>

      <template #footer>
        <div class="flex flex-col sm:flex-row gap-3 sm:justify-end">
          <AppButton color="neutral"
                     icon="i-lucide-arrow-left"
                     size="lg"
                     to="/auth"
                     variant="outline">
            {{ t('auth.twoFactor.challenge.backToSignIn') }}
          </AppButton>
        </div>
      </template>
    </UPageCard>
  </UContainer>
</template>
