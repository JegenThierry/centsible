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

interface Props {
  initialValues: UserProfileForm;
  loading?: boolean;
}

const props = defineProps<Props>();
const emit = defineEmits(['save', 'validation-failed']);

const firstNameInput = ref<InstanceType<typeof BaseInput>>();
const lastNameInput = ref<InstanceType<typeof BaseInput>>();
const emailInput = ref<InstanceType<typeof BaseInput>>();

const state = reactive({...props.initialValues});

function validate(): boolean {
  const inputs = [
    firstNameInput.value,
    lastNameInput.value,
    emailInput.value
  ];

  let valid = true;
  inputs.forEach((input) => {
    if (input && !input.validate()) {
      valid = false;
    }
  });

  return valid;
}

async function onSubmit() {
  if (!validate()) {
    emit('validation-failed');
    return;
  }
  emit('save', state);
}
</script>
