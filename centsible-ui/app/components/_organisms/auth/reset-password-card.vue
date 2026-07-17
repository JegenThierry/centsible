<script lang="ts" setup>
import {useApi} from "~/composables/use-api";
import {useAuthService} from "~/services/auth/auth-service";
import {useToasts} from "~/services/toasts/toast-service";
import {z} from 'zod'
import type {FormSubmitEvent} from '@nuxt/ui'
import RegisterPasswordInput from "~/components/_molecules/inputs/register-password-input.vue";
import AppButton from "~/components/_atoms/ui/app-button.vue";
import {useApiErrors} from "~/composables/use-api-errors";

const {t} = useI18n();

const route = useRoute();
const api = useApi();
const {success, error} = useToasts();
const {toastError} = useApiErrors();

const token = computed(() => (typeof route.query.token === 'string' ? route.query.token : ''));
const tokenMissing = computed(() => token.value.length === 0);

const state = reactive({
  password: '',
  confirmPassword: '',
});
const loading = ref<boolean>(false);
const completed = ref<boolean>(false);
const linkInvalid = ref<boolean>(false);

const passwordLabel = t('auth.fields.password');
const confirmPasswordLabel = t('auth.fields.confirmPassword');

const schema = z.object({
  password: z.string()
    .min(1, t('common.validation.required', {field: passwordLabel}))
    .refine(isStrongPassword, t('auth.password.doesNotMeetRequirements')),
  confirmPassword: z.string().min(1, t('common.validation.required', {field: confirmPasswordLabel})),
}).refine((d) => d.password === d.confirmPassword, {
  message: t('auth.password.doNotMatch'),
  path: ['confirmPassword'],
})
type Schema = z.output<typeof schema>

function onSubmit(_event: FormSubmitEvent<Schema>) {
  if (tokenMissing.value) {
    linkInvalid.value = true;
    return;
  }
  loading.value = true;
  useAuthService(api)
    .resetPassword(token.value, state.password)
    .then(async (ok) => {
      if (ok) {
        completed.value = true;
        success(t('auth.resetPassword.toastSuccessTitle'), t('auth.resetPassword.toastSuccessBody'));
        return;
      }
      linkInvalid.value = true;
      error(t('auth.resetPassword.toastErrorTitle'), t('auth.resetPassword.toastInvalidLinkBody'));
    })
    .catch((err) => {
      toastError(err, t('auth.resetPassword.toastErrorTitle'), t('auth.resetPassword.toastErrorBody'));
    })
    .finally(() => loading.value = false);
}

const cardState = computed<'form' | 'invalid' | 'success'>(() => {
  if (completed.value) return 'success';
  if (linkInvalid.value || tokenMissing.value) return 'invalid';
  return 'form';
});

const cardMeta = computed(() => {
  switch (cardState.value) {
    case 'success':
      return {
        icon: 'i-lucide-circle-check',
        iconClass: 'text-(--ui-success)',
        title: t('auth.resetPassword.successTitle'),
        description: t('auth.resetPassword.successDescription'),
      };
    case 'invalid':
      return {
        icon: 'i-lucide-circle-alert',
        iconClass: 'text-(--ui-error)',
        title: t('auth.resetPassword.invalidTitle'),
        description: t('auth.resetPassword.invalidDescription'),
      };
    default:
      return {
        icon: 'i-lucide-key-round',
        iconClass: 'text-primary',
        title: t('auth.resetPassword.title'),
        description: t('auth.resetPassword.description'),
      };
  }
});
</script>

<template>
  <UContainer class="py-12">
    <UPageCard
      :description="cardMeta.description"
      :icon="cardMeta.icon"
      :title="cardMeta.title"
      :ui="{ leadingIcon: cardMeta.iconClass }"
      class="max-w-xl mx-auto"
      spotlight
      spotlight-color="primary">
      <UForm v-if="cardState === 'form'" :schema="schema" :state="state" class="space-y-6 pt-4 flex flex-col" @submit="onSubmit">
        <RegisterPasswordInput v-model:confirm-password="state.confirmPassword"
                               v-model:password="state.password"/>

        <AppButton :loading="loading" class="ml-auto" type="submit">
          {{ t('auth.resetPassword.submit') }}
        </AppButton>
      </UForm>

      <template v-if="cardState !== 'form'" #footer>
        <div class="flex flex-col sm:flex-row gap-3 sm:justify-end">
          <AppButton v-if="cardState === 'invalid'"
                   color="neutral"
                   icon="i-lucide-key-round"
                   size="lg"
                   to="/auth/forgot-password"
                   variant="outline">
            {{ t('auth.resetPassword.requestNew') }}
          </AppButton>
          <AppButton color="primary"
                   icon="i-lucide-log-in"
                   size="lg"
                   to="/auth"
                   trailing-icon="i-lucide-arrow-right">
            {{ t('auth.resetPassword.continueToSignIn') }}
          </AppButton>
        </div>
      </template>
    </UPageCard>
  </UContainer>
</template>
