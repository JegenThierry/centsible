<script lang="ts" setup>

import PasswordInput from "~/components/_atoms/inputs/password-input.vue";

defineProps<{
  disabled?: boolean;
}>();

const {t} = useI18n();
const password = defineModel<string>('password', {required: true});
const passwordInput = ref<InstanceType<typeof PasswordInput>>();

const confirmPassword = defineModel<string>('confirm-password', {required: true});
const confirmPasswordInput = ref<InstanceType<typeof PasswordInput>>();

const validationMessage = ref<string>();

function arePasswordsEqual(): boolean {
  return password.value === confirmPassword.value;
}

const passwordRules = computed(() => [
  {label: t('auth.password.rules.length'), met: password.value.length >= 8},
  {label: t('auth.password.rules.case'), met: /[A-Z]/.test(password.value) && /[a-z]/.test(password.value)},
  {label: t('auth.password.rules.digit'), met: /\d/.test(password.value)},
  {label: t('auth.password.rules.special'), met: /[@$!%*?&]/.test(password.value)}
])

function passwordMatchesSecuritySettings() {
  return passwordRules.value.every(rule => rule.met)
}

function validatePasswordData() {
  if (!arePasswordsEqual()) {
    validationMessage.value = t('auth.password.doNotMatch');
    return false;
  }

  if (!passwordMatchesSecuritySettings()) {
    validationMessage.value = t('auth.password.doesNotMeetRequirements');
    return false;
  }

  return true;
}

function validate(): boolean {
  if (!passwordInput.value || !confirmPasswordInput.value) return false;
  const passwordValid = passwordInput.value.validate();
  const confirmPasswordValid = confirmPasswordInput.value.validate();
  return passwordValid && confirmPasswordValid;
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
                  :disabled="disabled"
                  :label="t('auth.fields.password')"
                  :placeholder="t('auth.placeholders.password')"
                  required/>

  <div class="grid grid-cols-2 gap-2">
    <div v-for="rule in passwordRules"
         :key="rule.label"
         :class="rule.met ? 'text-primary-500' : 'text-gray-400 dark:text-gray-500'"
         class="flex items-center gap-2 text-xs transition-colors duration-200">
      <UIcon :name="rule.met ? 'i-lucide-circle-check' : 'i-lucide-circle-minus'"
             class="w-4 h-4"/>
      {{ rule.label }}
    </div>
  </div>

  <password-input ref="confirmPasswordInput"
                  v-model="confirmPassword"
                  :additional-validation="validatePasswordData"
                  :additional-validation-message="validationMessage"
                  :disabled="disabled"
                  :label="t('auth.fields.confirmPassword')"
                  :placeholder="t('auth.placeholders.confirmPassword')"
                  required/>
</template>
