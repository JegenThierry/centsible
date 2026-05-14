<template>
  <UForm :state="state" class="space-y-6 pt-4 flex flex-col" @submit="onSubmit">
    <BaseInput
      ref="firstNameInput"
      v-model="state.firstName"
      label="First Name"
      placeholder="First Name"
      required
      type="text"
    />

    <BaseInput
      ref="lastNameInput"
      v-model="state.lastName"
      label="Last Name"
      placeholder="Last Name"
      required
      type="text"
    />

    <BaseInput
      ref="emailInput"
      v-model="state.email"
      label="Email"
      placeholder="Email"
      required
      type="email"
    />

    <UButton :loading="loading" class="ml-auto" type="submit">
      Save Changes
    </UButton>
  </UForm>
</template>

<script lang="ts" setup>
import BaseInput from "~/components/_atoms/inputs/base-input.vue";
import type {UserProfileForm} from "~/models/user/user-profile-form";
import {useValidator} from "~/composables/use-validator";

interface Props {
  initialValues?: UserProfileForm;
  loading?: boolean;
}

const props = defineProps<Props>();
const emit = defineEmits<{
  (e: 'save', value: UserProfileForm): void;
  (e: 'validation-failed'): void;
}>();

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

function validate(): boolean {
  return useValidator().validateInputs([firstNameInput, lastNameInput, emailInput]);
}

async function onSubmit() {
  if (!validate()) {
    emit('validation-failed');
    return;
  }
  emit('save', state);
}
</script>
