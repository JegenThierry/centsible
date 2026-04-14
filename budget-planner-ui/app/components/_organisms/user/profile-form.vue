<template>
  <UForm :state="state" class="space-y-6 pt-4 flex flex-col" @submit="onSubmit">
    <BaseInput
      v-model="state.firstName"
      ref="firstNameInput"
      label="First Name"
      placeholder="First Name"
      type="text"
      required
    />

    <BaseInput
      v-model="state.lastName"
      ref="lastNameInput"
      label="Last Name"
      placeholder="Last Name"
      type="text"
      required
    />

    <BaseInput
      v-model="state.email"
      ref="emailInput"
      label="Email"
      placeholder="Email"
      type="email"
      required
    />

    <UButton class="ml-auto" :loading="loading" type="submit">
      Save Changes
    </UButton>
  </UForm>
</template>

<script setup lang="ts">
import BaseInput from "~/components/_atoms/inputs/base-input.vue";

interface Props {
  initialValues: {
    firstName: string;
    lastName: string;
    email: string;
  };
  loading?: boolean;
}

const props = defineProps<Props>();
const emit = defineEmits(['save', 'validation-failed']);

const firstNameInput = ref<InstanceType<typeof BaseInput>>();
const lastNameInput = ref<InstanceType<typeof BaseInput>>();
const emailInput = ref<InstanceType<typeof BaseInput>>();

const state = reactive({ ...props.initialValues });

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
