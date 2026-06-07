<script lang="ts" setup>
import {z} from 'zod'
import type {FormSubmitEvent} from '@nuxt/ui'
import {useApi} from "~/composables/use-api";
import {useAuthService} from "~/services/auth/auth-service";
import {useToasts} from "~/services/toasts/toast-service";
import BaseInput from "~/components/_atoms/inputs/base-input.vue";
import RegisterPasswordInput from "~/components/_molecules/inputs/register-password-input.vue";
import AppButton from "~/components/_atoms/ui/app-button.vue";
import {useApiErrors} from "~/composables/use-api-errors";

import {useUserStore} from "~/stores/userStore";

const api = useApi();
const authStore = useAuthStore();
const userStore = useUserStore();
const {success} = useToasts();
const {t, locale} = useI18n();

const state = reactive({
  username: '',
  email: '',
  firstName: '',
  lastName: '',
  password: '',
  confirmPassword: '',
})
const loading = ref<boolean>(false);

const usernameLabel = t('auth.fields.username');
const emailLabel = t('auth.fields.email');
const firstNameLabel = t('auth.fields.firstName');
const lastNameLabel = t('auth.fields.lastName');
const passwordLabel = t('auth.fields.password');
const confirmPasswordLabel = t('auth.fields.confirmPassword');

const schema = z.object({
  username: z.string().trim()
    .min(1, t('common.validation.required', {field: usernameLabel}))
    .min(3, t('common.validation.minLength', {field: usernameLabel, min: 3}))
    .max(50, t('common.validation.maxLength', {field: usernameLabel, max: 50}))
    .regex(USERNAME_PATTERN, t('auth.register.usernamePattern')),
  email: z.string().trim()
    .min(1, t('common.validation.required', {field: emailLabel}))
    .max(255, t('common.validation.maxLength', {field: emailLabel, max: 255}))
    .regex(EMAIL_REGEX, t('common.validation.email', {field: emailLabel})),
  firstName: z.string().trim()
    .min(1, t('common.validation.required', {field: firstNameLabel}))
    .max(100, t('common.validation.maxLength', {field: firstNameLabel, max: 100})),
  lastName: z.string().trim()
    .min(1, t('common.validation.required', {field: lastNameLabel}))
    .max(100, t('common.validation.maxLength', {field: lastNameLabel, max: 100})),
  password: z.string()
    .min(1, t('common.validation.required', {field: passwordLabel}))
    .refine(
      (v) => v.length >= 8 && /[A-Z]/.test(v) && /[a-z]/.test(v) && /\d/.test(v) && /[@$!%*?&]/.test(v),
      t('auth.password.doesNotMeetRequirements'),
    ),
  confirmPassword: z.string()
    .min(1, t('common.validation.required', {field: confirmPasswordLabel})),
}).refine((d) => d.password === d.confirmPassword, {
  message: t('auth.password.doNotMatch'),
  path: ['confirmPassword'],
})
type Schema = z.output<typeof schema>

function onSubmit(_event: FormSubmitEvent<Schema>) {
  if (loading.value) return;
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
</script>

<template>
  <UForm :schema="schema" :state="state" class="space-y-6 pt-4 flex flex-col" @submit="onSubmit">
    <BaseInput name="username"
               v-model="state.username"
               autofocus
               :disabled="loading"
               :label="usernameLabel"
               :placeholder="t('auth.placeholders.username')"
               required
               type="text"/>

    <BaseInput name="email"
               v-model="state.email"
               :disabled="loading"
               :label="emailLabel"
               :placeholder="t('auth.placeholders.email')"
               required
               type="email"/>

    <BaseInput name="firstName"
               v-model="state.firstName"
               :disabled="loading"
               :label="firstNameLabel"
               :placeholder="t('auth.placeholders.firstName')"
               required
               type="text"/>

    <BaseInput name="lastName"
               v-model="state.lastName"
               :disabled="loading"
               :label="lastNameLabel"
               :placeholder="t('auth.placeholders.lastName')"
               required
               type="text"/>

    <RegisterPasswordInput v-model:confirm-password="state.confirmPassword"
                           v-model:password="state.password"
                           :disabled="loading"/>

    <AppButton :loading="loading" class="ml-auto" type="submit">
      {{ t('auth.register.submit') }}
    </AppButton>
  </UForm>
</template>
