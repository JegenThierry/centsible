<script lang="ts" setup>
import {z} from 'zod'
import type {FormSubmitEvent} from '@nuxt/ui'
import PasswordInput from "~/components/_atoms/inputs/password-input.vue";
import AppButton from "~/components/_atoms/ui/app-button.vue";
import {useAuthStore} from "~/stores/authStore";
import {useToasts} from "~/services/toasts/toast-service";

const {t} = useI18n();
const authStore = useAuthStore();
const {success, error} = useToasts();

const state = reactive({currentPassword: '', newPassword: '', confirmPassword: ''});
const loading = ref(false);

const schema = z.object({
  currentPassword: z.string()
    .min(1, t('common.validation.required', {field: t('profile.password.currentLabel')})),
  newPassword: z.string()
    .min(1, t('common.validation.required', {field: t('profile.password.newLabel')}))
    .refine(isStrongPassword, t('profile.password.validation.tooWeak')),
  confirmPassword: z.string()
    .min(1, t('common.validation.required', {field: t('profile.password.confirmLabel')})),
})
  .refine((d) => d.newPassword !== d.currentPassword, {
    message: t('profile.password.validation.mustDiffer'),
    path: ['newPassword'],
  })
  .refine((d) => d.newPassword === d.confirmPassword, {
    message: t('profile.password.validation.mustMatch'),
    path: ['confirmPassword'],
  })
type Schema = z.output<typeof schema>

const passwordRules = computed(() =>
  PASSWORD_RULES.map((rule) => ({label: t(rule.labelKey), met: rule.test(state.newPassword)})),
);

function reset() {
  state.currentPassword = '';
  state.newPassword = '';
  state.confirmPassword = '';
}

async function onSubmit(_event: FormSubmitEvent<Schema>) {
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

    <UForm :schema="schema" :state="state" class="mt-4 space-y-4" @submit="onSubmit">
      <PasswordInput name="currentPassword"
                     v-model="state.currentPassword"
                     required
                     :label="t('profile.password.currentLabel')"
                     :placeholder="t('profile.password.currentPlaceholder')"/>
      <PasswordInput name="newPassword"
                     v-model="state.newPassword"
                     required
                     :label="t('profile.password.newLabel')"
                     :placeholder="t('profile.password.newPlaceholder')"
                     :description="t('profile.password.newHint')"/>
      <div v-if="state.newPassword.length > 0" class="grid grid-cols-2 gap-2 -mt-2">
        <div v-for="rule in passwordRules"
             :key="rule.label"
             :class="rule.met ? 'text-primary-500' : 'text-muted'"
             class="flex items-center gap-2 text-xs transition-colors duration-200">
          <UIcon :name="rule.met ? 'i-lucide-circle-check' : 'i-lucide-circle-minus'" class="w-4 h-4"/>
          {{ rule.label }}
        </div>
      </div>
      <PasswordInput name="confirmPassword"
                     v-model="state.confirmPassword"
                     required
                     :label="t('profile.password.confirmLabel')"
                     :placeholder="t('profile.password.confirmPlaceholder')"/>
      <div class="flex">
        <AppButton class="ml-auto" type="submit" :loading="loading">
          {{ t('profile.password.submit') }}
        </AppButton>
      </div>
    </UForm>
  </div>
</template>
