<script lang="ts" setup>
import {z} from 'zod'
import type {FormSubmitEvent} from '@nuxt/ui'
import {useApi} from "~/composables/use-api";
import {useSetupService} from "~/services/setup/setup-service";
import {useToasts} from "~/services/toasts/toast-service";
import {useApiErrors} from "~/composables/use-api-errors";
import {useUserStore} from "~/stores/userStore";
import BaseInput from "~/components/_atoms/inputs/base-input.vue";
import RegisterPasswordInput from "~/components/_molecules/inputs/register-password-input.vue";
import AppButton from "~/components/_atoms/ui/app-button.vue";

const api = useApi();
const authStore = useAuthStore();
const userStore = useUserStore();
const {success} = useToasts();
const {toastError} = useApiErrors();
const {t, locale} = useI18n();

const state = reactive({
  username: '',
  email: '',
  firstName: '',
  lastName: '',
  password: '',
  confirmPassword: '',
})
const loading = ref(false);

const usernameLabel = t('auth.fields.username');
const emailLabel = t('auth.fields.email');
const firstNameLabel = t('auth.fields.firstName');
const lastNameLabel = t('auth.fields.lastName');

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
    .min(1, t('common.validation.required', {field: t('auth.fields.password')}))
    .refine(isStrongPassword, t('auth.password.doesNotMeetRequirements')),
  confirmPassword: z.string()
    .min(1, t('common.validation.required', {field: t('auth.fields.confirmPassword')})),
}).refine((d) => d.password === d.confirmPassword, {
  message: t('auth.password.doNotMatch'),
  path: ['confirmPassword'],
})
type Schema = z.output<typeof schema>

function onSubmit(_event: FormSubmitEvent<Schema>) {
  if (loading.value) return;
  loading.value = true;
  useSetupService(api)
    .create({
      username: state.username,
      email: state.email,
      firstName: state.firstName,
      lastName: state.lastName,
      password: state.password,
      locale: locale.value,
    })
    .then(async () => {
      authStore.setAuthenticated(true);
      await userStore.fetchMyselfBestEffort();
      success(t('auth.setup.toastSuccessTitle'), t('auth.setup.toastSuccessBody'));
      await navigateTo('/accounts');
    })
    .catch((err) => {
      toastError(err, t('auth.setup.toastErrorTitle'), t('auth.setup.toastErrorFallback'));
    })
    .finally(() => loading.value = false);
}
</script>

<template>
  <UContainer class="py-12">
    <UPageCard :description="t('auth.setup.description')"
               :title="t('auth.setup.title')"
               class="max-w-xl mx-auto"
               icon="i-lucide-rocket"
               spotlight
               spotlight-color="primary">
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
          {{ t('auth.setup.submit') }}
        </AppButton>
      </UForm>
    </UPageCard>
  </UContainer>
</template>
