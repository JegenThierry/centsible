<template>
  <UForm :state="state" class="space-y-6 pt-4 flex flex-col" @submit="onSubmit">
    <BaseInput
      ref="firstNameInput"
      v-model="state.firstName"
      :max-length="100"
      :disabled="loading"
      :label="t('profile.form.firstNameLabel')"
      :placeholder="t('profile.form.firstNamePlaceholder')"
      required
      type="text"
    />

    <BaseInput
      ref="lastNameInput"
      v-model="state.lastName"
      :max-length="100"
      :disabled="loading"
      :label="t('profile.form.lastNameLabel')"
      :placeholder="t('profile.form.lastNamePlaceholder')"
      required
      type="text"
    />

    <BaseInput
      ref="emailInput"
      v-model="state.email"
      :max-length="255"
      :disabled="loading"
      :label="t('profile.form.emailLabel')"
      :placeholder="t('profile.form.emailPlaceholder')"
      required
      type="email"
    />

    <UButton :loading="loading" class="ml-auto" type="submit">
      {{ t('profile.form.submit') }}
    </UButton>
  </UForm>
</template>

<script lang="ts" setup>
import BaseInput from "~/components/_atoms/inputs/base-input.vue";
import type {UserProfileForm} from "~/models/user/user-profile-form";
import {useValidator} from "~/composables/use-validator";
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

const firstNameInput = ref<InstanceType<typeof BaseInput>>();
const lastNameInput = ref<InstanceType<typeof BaseInput>>();
const emailInput = ref<InstanceType<typeof BaseInput>>();

const state = reactive<UserProfileForm>({
  firstName: props.initialValues?.firstName ?? '',
  lastName: props.initialValues?.lastName ?? '',
  email: props.initialValues?.email ?? '',
});

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

function validate(): boolean {
  return useValidator().validateInputs([firstNameInput, lastNameInput, emailInput]);
}

async function onSubmit() {
  if (props.loading) return;
  if (!validate()) {
    emit('validation-failed');
    return;
  }
  emit('save', state);
}
</script>
