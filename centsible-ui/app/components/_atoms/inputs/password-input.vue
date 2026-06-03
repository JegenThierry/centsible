<script lang="ts" setup>
import AppInput from "~/components/_atoms/ui/app-input.vue";
import AppButton from "~/components/_atoms/ui/app-button.vue";

const props = defineProps<{
  required?: boolean;
  disabled?: boolean;
  label?: string;
  placeholder?: string;
  description?: string;
  additionalValidation?: () => boolean;
  additionalValidationMessage?: string;
}>();

const show = ref(false);
const error = ref<string | undefined>(undefined);
const password = defineModel<string>({required: true});
const {t} = useI18n();

function validate(): boolean {
  error.value = undefined;

  if (props.required && !password.value) {
    error.value = t('common.validation.required', {field: props.label ?? t('auth.password.label')});
    return false;
  }

  if (props.additionalValidation && !props.additionalValidation()) {
    error.value = props.additionalValidationMessage ?? t('common.states.error');
    return false;
  }

  return true;
}

defineExpose({validate});
</script>

<template>
  <UFormField :error="error"
              :help="description"
              :label="label"
              :required="required">
    <AppInput
      v-model="password"
      :disabled="disabled"
      :placeholder="placeholder"
      :type="show ? 'text' : 'password'"
      :ui="{ trailing: 'pe-1' }"
      class="w-full"
    >
      <template #trailing>
        <AppButton
          :aria-label="show ? t('auth.password.hide') : t('auth.password.show')"
          :aria-pressed="show"
          :disabled="disabled"
          :icon="show ? 'i-lucide-eye-off' : 'i-lucide-eye'"
          aria-controls="password"
          color="neutral"
          size="sm"
          variant="link"
          @click="show = !show"
        />
      </template>
    </AppInput>
  </UFormField>
</template>
