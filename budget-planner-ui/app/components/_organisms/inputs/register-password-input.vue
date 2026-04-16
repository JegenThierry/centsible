<script lang="ts" setup>

import PasswordInput from "~/components/_atoms/inputs/password-input.vue";

const password = defineModel<string>('password', {required: true});
const passwordInput = ref<InstanceType<typeof PasswordInput>>();

const confirmPassword = defineModel<string>('confirm-password', {required: true});
const confirmPasswordInput = ref<InstanceType<typeof PasswordInput>>();

const validationMessage = ref<string>();

function arePasswordsEqual(): boolean {
  return password.value === confirmPassword.value;
}

/**
 * The password needs to match the following criteria:
 * - Needs to be at least 8 chars long
 * - Needs to contain at least 1 uppercase letter
 * - Needs to contain at least 1 lowercase letter
 * - Needs to contain at least 1 number
 * - Needs to contain at least 1 of the following special characters: @ $ ! % * ? &
 */
const passwordRules = computed(() => [
  {label: '8+ characters', met: password.value.length >= 8},
  {label: 'Upper & lowercase', met: /[A-Z]/.test(password.value) && /[a-z]/.test(password.value)},
  {label: 'At least one number', met: /\d/.test(password.value)},
  {label: 'Special symbol (@$!%*?&)', met: /[@$!%*?&]/.test(password.value)}
])

function passwordMatchesSecuritySettings() {
  return passwordRules.value.every(rule => rule.met)
}

function validatePasswordData() {
  if (!arePasswordsEqual()) {
    validationMessage.value = "Passwords do not match";
    return false;
  }

  if (!passwordMatchesSecuritySettings()) {
    validationMessage.value = "Password does not meet requirements";
    return false;
  }

  return true;
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
  <password-input ref="passwordInput"
                  v-model="password"
                  :additional-validation="validatePasswordData"
                  :additional-validation-message="validationMessage"
                  label="Password"
                  placeholder="Enter a password"
                  required/>

  <div class="grid grid-cols-2 gap-2">
    <div v-for="rule in passwordRules"
         :key="rule.label"
         :class="rule.met ? 'text-primary-500' : 'text-gray-400 dark:text-gray-500'"
         class="flex items-center gap-2 text-xs transition-colors duration-200">
      <UIcon :name="rule.met ? 'i-heroicons-check-circle-20-solid' : 'i-heroicons-minus-circle'"
             class="w-4 h-4"/>
      {{ rule.label }}
    </div>
  </div>

  <password-input ref="confirmPasswordInput"
                  v-model="confirmPassword"
                  :additional-validation="validatePasswordData"
                  :additional-validation-message="validationMessage"
                  label="Confirm Password"
                  placeholder="Confirm your password"
                  required/>
</template>

<style scoped>

</style>
