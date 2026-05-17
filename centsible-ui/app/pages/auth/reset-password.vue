<script lang="ts" setup>
import {useApi} from "~/composables/use-api";
import {useAuthService} from "~/services/auth/auth-service";
import {useToasts} from "~/services/toasts/toast-service";
import RegisterPasswordInput from "~/components/_organisms/inputs/register-password-input.vue";
import {useValidator} from "~/composables/use-validator";
import {useApiErrors} from "~/composables/use-api-errors";

definePageMeta({
  middleware: [
    () => {
      const authStore = useAuthStore();
      if (authStore.isAuthenticated) {
        return navigateTo('/accounts');
      }
    },
  ],
});

const {t} = useI18n();
useHead({
  title: t('auth.resetPassword.pageTitle'),
});

const route = useRoute();
const api = useApi();
const {success, error} = useToasts();

const token = computed(() => (typeof route.query.token === 'string' ? route.query.token : ''));
const tokenMissing = computed(() => token.value.length === 0);

const state = reactive({
  password: '',
  confirmPassword: '',
});
const loading = ref<boolean>(false);
const completed = ref<boolean>(false);
const linkInvalid = ref<boolean>(false);

const passwordsInput = ref<InstanceType<typeof RegisterPasswordInput>>();

function validate(): boolean {
  const valid = useValidator().validateInputs([passwordsInput]);
  if (!valid) {
    error(t('auth.resetPassword.toastValidationTitle'), t('auth.resetPassword.toastValidationBody'));
  }
  return valid;
}

function onSubmit() {
  if (tokenMissing.value) {
    linkInvalid.value = true;
    return;
  }
  if (!validate()) {
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
      useApiErrors().toastError(err, t('auth.resetPassword.toastErrorTitle'), t('auth.resetPassword.toastErrorBody'));
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
      <UForm v-if="cardState === 'form'" :state="state" class="space-y-6 pt-4 flex flex-col" @submit="onSubmit">
        <RegisterPasswordInput ref="passwordsInput"
                               v-model:confirm-password="state.confirmPassword"
                               v-model:password="state.password"/>

        <UButton :loading="loading" class="ml-auto" type="submit">
          {{ t('auth.resetPassword.submit') }}
        </UButton>
      </UForm>

      <template v-if="cardState !== 'form'" #footer>
        <div class="flex flex-col sm:flex-row gap-3 sm:justify-end">
          <UButton v-if="cardState === 'invalid'"
                   color="neutral"
                   icon="i-lucide-key-round"
                   size="lg"
                   to="/auth/forgot-password"
                   variant="outline">
            {{ t('auth.resetPassword.requestNew') }}
          </UButton>
          <UButton color="primary"
                   icon="i-lucide-log-in"
                   size="lg"
                   to="/auth"
                   trailing-icon="i-lucide-arrow-right">
            {{ t('auth.resetPassword.continueToSignIn') }}
          </UButton>
        </div>
      </template>
    </UPageCard>
  </UContainer>
</template>
