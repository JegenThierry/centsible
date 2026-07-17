<script lang="ts" setup>
import {z} from 'zod'
import type {FormSubmitEvent} from '@nuxt/ui'
import {useApi} from "~/composables/use-api";
import {useAuthService} from "~/services/auth/auth-service";
import {useToasts} from "~/services/toasts/toast-service";
import {useApiErrors} from "~/composables/use-api-errors";
import BaseInput from "~/components/_atoms/inputs/base-input.vue";
import PasswordInput from "~/components/_atoms/inputs/password-input.vue";
import AppButton from "~/components/_atoms/ui/app-button.vue";

const api = useApi();
const authStore = useAuthStore();
const userStore = useUserStore();
const {success} = useToasts();
const {toastError} = useApiErrors();
const {t} = useI18n();

const state = reactive({
  username: '',
  password: '',
})
const loading = ref<boolean>(false);

const schema = z.object({
  username: z.string().trim().min(1, t('common.validation.required', {field: t('auth.fields.username')})),
  password: z.string().min(1, t('common.validation.required', {field: t('auth.fields.password')})),
})
type Schema = z.output<typeof schema>

function onSubmit(_event: FormSubmitEvent<Schema>) {
  if (loading.value) return;
  loading.value = true;
  useAuthService(api)
    .login({username: state.username, password: state.password})
    .then(async (result) => {
      if (result.twoFactorRequired) {
        authStore.setTwoFactorPending(true);
        await navigateTo('/auth/2fa');
        return;
      }
      authStore.setAuthenticated(true);
      await userStore.fetchMyselfBestEffort();
      success(t('auth.login.toastSuccessTitle'), t('auth.login.toastSuccessBody'));
      await navigateTo('/accounts');
    })
    .catch((err) => {
      toastError(err, t('auth.login.toastErrorTitle'), t('auth.login.toastErrorGeneric'));
    })
    .finally(() => loading.value = false);
}
</script>

<template>
  <UForm :schema="schema" :state="state" class="space-y-6 pt-4 flex flex-col" @submit="onSubmit">
    <BaseInput name="username"
               v-model="state.username"
               autofocus
               autocomplete="username"
               :disabled="loading"
               :label="t('auth.fields.username')"
               required
               type="text"/>

    <PasswordInput name="password"
                   v-model="state.password"
                   autocomplete="current-password"
                   :disabled="loading"
                   :label="t('auth.fields.password')"
                   required/>

    <div class="flex justify-end">
      <AppButton color="neutral"
               size="sm"
               to="/auth/forgot-password"
               variant="link"
               class="px-0">
        {{ t('auth.login.forgotPassword') }}
      </AppButton>
    </div>

    <AppButton :loading="loading" class="ml-auto" type="submit">
      {{ t('auth.login.submit') }}
    </AppButton>
  </UForm>
</template>
