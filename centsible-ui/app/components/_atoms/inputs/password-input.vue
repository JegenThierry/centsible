<script lang="ts" setup>
import AppInput from "~/components/_atoms/ui/app-input.vue";

const props = defineProps<{
  /** When set, the field is schema-driven: UForm owns validation and shows the error for this path. */
  name?: string;
  required?: boolean;
  disabled?: boolean;
  label?: string;
  placeholder?: string;
  description?: string;
  additionalValidation?: () => boolean;
  additionalValidationMessage?: string;
  autocomplete?: string;
}>();

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
  <UFormField :name="name"
              :error="error"
              :help="description"
              :label="label"
              :required="required">
    <AppInput
      v-model="password"
      :disabled="disabled"
      :placeholder="placeholder"
      :type="'password'"
      :autocomplete="autocomplete"
      :ui="{ trailing: 'pe-1' }"
      class="w-full"/>
  </UFormField>
</template>
