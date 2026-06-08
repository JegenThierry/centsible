<script lang="ts" setup>
import PasswordInput from "~/components/_atoms/inputs/password-input.vue";

defineProps<{
  disabled?: boolean;
}>();

const {t} = useI18n();
const password = defineModel<string>('password', {required: true});
const confirmPassword = defineModel<string>('confirm-password', {required: true});

const passwordRules = computed(() => [
  {label: t('auth.password.rules.length'), met: password.value.length >= 8},
  {label: t('auth.password.rules.case'), met: /[A-Z]/.test(password.value) && /[a-z]/.test(password.value)},
  {label: t('auth.password.rules.digit'), met: /\d/.test(password.value)},
  {label: t('auth.password.rules.special'), met: /[@$!%*?&]/.test(password.value)}
])
</script>

<template>
  <password-input name="password"
                  v-model="password"
                  autocomplete="new-password"
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

  <password-input name="confirmPassword"
                  v-model="confirmPassword"
                  autocomplete="new-password"
                  :disabled="disabled"
                  :label="t('auth.fields.confirmPassword')"
                  :placeholder="t('auth.placeholders.confirmPassword')"
                  required/>
</template>
