<template>
  <UForm :schema="schema" :state="state" class="space-y-6 pt-4 flex flex-col" @submit="onSubmit" @error="emit('validation-failed')">
    <BaseInput
      name="firstName"
      v-model="state.firstName"
      :max-length="100"
      :disabled="loading"
      :label="t('profile.form.firstNameLabel')"
      :placeholder="t('profile.form.firstNamePlaceholder')"
      required
      type="text"
    />

    <BaseInput
      name="lastName"
      v-model="state.lastName"
      :max-length="100"
      :disabled="loading"
      :label="t('profile.form.lastNameLabel')"
      :placeholder="t('profile.form.lastNamePlaceholder')"
      required
      type="text"
    />

    <BaseInput
      name="email"
      v-model="state.email"
      :max-length="255"
      :disabled="loading"
      :label="t('profile.form.emailLabel')"
      :placeholder="t('profile.form.emailPlaceholder')"
      required
      type="email"
    />

    <AppButton :loading="loading" class="ml-auto" type="submit">
      {{ t('profile.form.submit') }}
    </AppButton>
  </UForm>
</template>

<script lang="ts" setup>
import {z} from 'zod'
import type {FormSubmitEvent} from '@nuxt/ui'
import BaseInput from "~/components/_atoms/inputs/base-input.vue";
import AppButton from "~/components/_atoms/ui/app-button.vue";
import type {UserProfileForm} from "~/models/user/user-profile-form";
import {useUnsavedChangesGuard} from "~/composables/use-unsaved-changes-guard";

interface Props {
  initialValues?: UserProfileForm;
  loading?: boolean;
}

const props = defineProps<Props>();
const emit = defineEmits<{
  (e: 'save', value: UserProfileForm): void;
  (e: 'validation-failed'): void;
}>();

const {t} = useI18n();

const firstNameLabel = t('profile.form.firstNameLabel');
const lastNameLabel = t('profile.form.lastNameLabel');
const emailLabel = t('profile.form.emailLabel');

const state = reactive<UserProfileForm>({
  firstName: props.initialValues?.firstName ?? '',
  lastName: props.initialValues?.lastName ?? '',
  email: props.initialValues?.email ?? '',
});

const schema = z.object({
  firstName: z.string().trim()
    .min(1, t('common.validation.required', {field: firstNameLabel}))
    .max(100, t('common.validation.maxLength', {field: firstNameLabel, max: 100})),
  lastName: z.string().trim()
    .min(1, t('common.validation.required', {field: lastNameLabel}))
    .max(100, t('common.validation.maxLength', {field: lastNameLabel, max: 100})),
  email: z.string().trim()
    .min(1, t('common.validation.required', {field: emailLabel}))
    .max(255, t('common.validation.maxLength', {field: emailLabel, max: 255}))
    .regex(EMAIL_REGEX, t('common.validation.email', {field: emailLabel})),
})
type Schema = z.output<typeof schema>

watch(
  () => props.initialValues,
  (next) => {
    if (next) Object.assign(state, next);
  },
);

const isDirty = computed(() => {
  const base = props.initialValues;
  if (!base) return false;
  if (props.loading) return false;
  return state.firstName !== base.firstName
    || state.lastName !== base.lastName
    || state.email !== base.email;
});

useUnsavedChangesGuard(isDirty);

function onSubmit(_event: FormSubmitEvent<Schema>) {
  if (props.loading) return;
  emit('save', state);
}
</script>
