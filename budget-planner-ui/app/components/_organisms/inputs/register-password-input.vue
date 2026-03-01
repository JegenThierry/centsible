<script setup lang="ts">

import PasswordInput from "~/components/_atoms/inputs/password-input.vue";

const password = defineModel<string>('password', {required: true});
const passwordInput = ref<InstanceType<typeof PasswordInput>>();

const confirmPassword = defineModel<string>('confirm-password', {required: true});
const confirmPasswordInput = ref<InstanceType<typeof PasswordInput>>();

function arePasswordsEqual(): boolean {
  return password.value === confirmPassword.value;
}

function validate(): boolean {
  if (!passwordInput.value || !confirmPasswordInput.value) {
    return false;
  }

  const passwordValid = passwordInput.value.validate();
  const confirmPasswordValid = confirmPasswordInput.value.validate();
  return passwordValid && confirmPasswordValid
}

defineExpose({
  validate,
})
</script>

<template>
  <password-input v-model="password"
                  ref="passwordInput"
                  label="Password"
                  required
                  placeholder="Enter a password"
                  additional-validation-message="Passwords do not match."
                  :additional-validation="arePasswordsEqual"/>

  <password-input v-model="confirmPassword"
                  ref="confirmPasswordInput"
                  label="Confirm Password"
                  required
                  placeholder="Confirm your password"
                  additional-validation-message="Passwords do not match."
                  :additional-validation="arePasswordsEqual"/>
</template>

<style scoped>

</style>