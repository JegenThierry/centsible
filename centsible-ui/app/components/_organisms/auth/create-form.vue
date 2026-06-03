<script lang="ts" setup>
import {useApi} from "~/composables/use-api";
import {useAuthService} from "~/services/auth/auth-service";
import {useToasts} from "~/services/toasts/toast-service";
import BaseInput from "~/components/_atoms/inputs/base-input.vue";
import RegisterPasswordInput from "~/components/_molecules/inputs/register-password-input.vue";
import AppButton from "~/components/_atoms/ui/app-button.vue";
import {useValidator} from "~/composables/use-validator";
import {useApiErrors} from "~/composables/use-api-errors";

import {useUserStore} from "~/stores/userStore";

const api = useApi();
const authStore = useAuthStore();
const userStore = useUserStore();
const {success, error} = useToasts();
const {t, locale} = useI18n();

const state = reactive({
  username: '',
  email: '',
  firstName: '',
  lastName: '',
  password: '',
  confirmPassword: '',
})
const usernameInput = ref<InstanceType<typeof BaseInput>>();
const emailInput = ref<InstanceType<typeof BaseInput>>();
const firstnameInput = ref<InstanceType<typeof BaseInput>>();
const lastnameInput = ref<InstanceType<typeof BaseInput>>();
const passwordsInput = ref<InstanceType<typeof RegisterPasswordInput>>();

const loading = ref<boolean>(false);

function onSubmit() {
  if (loading.value) return;
  if (!validate()) return;
  loading.value = true;

  useAuthService(api)
    .register({
      username: state.username,
      email: state.email,
      firstName: state.firstName,
      lastName: state.lastName,
      password: state.password,
      locale: locale.value,
    })
    .then(async (res) => {
      if (res.token) {
        authStore.setAuthenticated(true);
        await userStore.fetchMyself();
        success(t('auth.register.toastSuccessTitle'), t('auth.register.toastSuccessBodyAuto'));
        await navigateTo('/accounts');
        return;
      }
      success(t('auth.register.toastSuccessTitle'), t('auth.register.toastSuccessBodyEmail'));
      await navigateTo({path: '/auth/check-email', query: {email: state.email}});
    })
    .catch((err) => {
      useApiErrors().toastError(err, t('auth.register.toastErrorTitle'), t('auth.register.toastErrorFallback'));
    })
    .finally(() => loading.value = false);
}

function validate(): boolean {
  const valid = useValidator().validateInputs([
    usernameInput,
    emailInput,
    firstnameInput,
    lastnameInput,
    passwordsInput,
  ]);
  if (!valid) error(t('auth.register.toastValidationTitle'), t('auth.register.toastValidationBody'));
  return valid;
}
</script>

<template>
  <UForm :state="state" class="space-y-6 pt-4 flex flex-col" @submit="onSubmit">
    <BaseInput ref="usernameInput"
               v-model="state.username"
               :max-length="50"
               :min-length="3"
               :pattern="USERNAME_PATTERN"
               autofocus
               :disabled="loading"
               :label="t('auth.fields.username')"
               :pattern-message="t('auth.register.usernamePattern')"
               :placeholder="t('auth.placeholders.username')"
               required
               type="text"/>

    <BaseInput ref="emailInput"
               v-model="state.email"
               :max-length="255"
               :disabled="loading"
               :label="t('auth.fields.email')"
               :placeholder="t('auth.placeholders.email')"
               required
               type="email"/>

    <BaseInput ref="firstnameInput"
               v-model="state.firstName"
               :max-length="100"
               :disabled="loading"
               :label="t('auth.fields.firstName')"
               :placeholder="t('auth.placeholders.firstName')"
               required
               type="text"/>

    <BaseInput ref="lastnameInput"
               v-model="state.lastName"
               :max-length="100"
               :disabled="loading"
               :label="t('auth.fields.lastName')"
               :placeholder="t('auth.placeholders.lastName')"
               required
               type="text"/>

    <RegisterPasswordInput ref="passwordsInput"
                           v-model:confirm-password="state.confirmPassword"
                           v-model:password="state.password"
                           :disabled="loading"/>

    <AppButton :loading="loading" class="ml-auto" type="submit">
      {{ t('auth.register.submit') }}
    </AppButton>
  </UForm>
</template>
