<script lang="ts" setup>
import {useApi} from "~/composables/use-api";
import {useAuthService} from "~/services/auth/auth-service";
import {useToasts} from "~/services/toasts/toast-service";
import BaseInput from "~/components/_atoms/inputs/base-input.vue";
import AppButton from "~/components/_atoms/ui/app-button.vue";
import {useValidator} from "~/composables/use-validator";
import {useApiErrors} from "~/composables/use-api-errors";

const {t} = useI18n();

const api = useApi();
const {success} = useToasts();

const state = reactive({
  username: '',
});
const loading = ref<boolean>(false);
const submitted = ref<boolean>(false);

const usernameInput = ref<InstanceType<typeof BaseInput>>();

function validate(): boolean {
  return useValidator().validateInputs([usernameInput]);
}

function onSubmit() {
  if (!validate()) {
    return;
  }
  loading.value = true;
  useAuthService(api)
    .forgotPassword(state.username)
    .then(() => {
      submitted.value = true;
      success(t('auth.forgotPassword.toastSuccessTitle'), t('auth.forgotPassword.toastSuccessBody'));
    })
    .catch((err) => {
      useApiErrors().toastError(err, t('auth.forgotPassword.toastErrorTitle'), t('auth.forgotPassword.toastErrorBody'));
    })
    .finally(() => loading.value = false);
}
</script>

<template>
  <UContainer class="py-12">
    <UPageCard
      :description="submitted ? t('auth.forgotPassword.submittedDescription') : t('auth.forgotPassword.description')"
      :icon="submitted ? 'i-lucide-mail-check' : 'i-lucide-key-round'"
      :title="submitted ? t('auth.forgotPassword.submittedTitle') : t('auth.forgotPassword.title')"
      :ui="{ leadingIcon: 'text-primary' }"
      class="max-w-xl mx-auto"
      spotlight
      spotlight-color="primary">
      <UForm v-if="!submitted" :state="state" class="space-y-6 pt-4 flex flex-col" @submit="onSubmit">
        <BaseInput ref="usernameInput"
                   v-model="state.username"
                   autofocus
                   :label="t('auth.fields.username')"
                   :placeholder="t('auth.placeholders.username')"
                   required
                   type="text"/>

        <AppButton :loading="loading" class="ml-auto" type="submit">
          {{ t('auth.forgotPassword.submit') }}
        </AppButton>
      </UForm>

      <p v-else class="text-sm text-(--ui-text-muted)">
        {{ t('auth.forgotPassword.expiryNote') }}
      </p>

      <template #footer>
        <div class="flex flex-col sm:flex-row gap-3 sm:justify-end">
          <AppButton color="neutral"
                   icon="i-lucide-arrow-left"
                   size="lg"
                   to="/auth"
                   variant="outline">
            {{ t('auth.forgotPassword.backToSignIn') }}
          </AppButton>
        </div>
      </template>
    </UPageCard>
  </UContainer>
</template>
