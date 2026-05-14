<script lang="ts" setup>
const props = defineProps<{
  label: string;
  description?: string;
  hint?: string;
  type: 'email' | 'number' | 'text';
  required?: boolean;
  placeholder?: string;
  additionalValidator?: () => string | undefined;
  autofocus?: boolean;
  min?: number;
  max?: number;
  minLength?: number;
  maxLength?: number;
  pattern?: RegExp;
  patternMessage?: string;
}>();

const model = defineModel<string | number>();
const error = ref<string | undefined>(undefined);

function isEmpty(value: string | number | undefined): boolean {
  if (value === undefined || value === null) return true;
  if (typeof value === 'string') return value.trim().length === 0;
  return false;
}

function validate(): boolean {
  error.value = undefined;

  const value = model.value;

  if (props.required && isEmpty(value)) {
    error.value = `${props.label} is required.`;
    return false;
  }

  if (isEmpty(value)) {
    return true;
  }

  const stringValue = typeof value === 'string' ? value : String(value);

  if (props.type === 'email' && !EMAIL_REGEX.test(stringValue.trim())) {
    error.value = `${props.label} must be a valid email address.`;
    return false;
  }

  if (props.minLength !== undefined && stringValue.length < props.minLength) {
    error.value = `${props.label} must be at least ${props.minLength} characters.`;
    return false;
  }

  if (props.maxLength !== undefined && stringValue.length > props.maxLength) {
    error.value = `${props.label} must be at most ${props.maxLength} characters.`;
    return false;
  }

  if (props.pattern && !props.pattern.test(stringValue)) {
    error.value = props.patternMessage ?? `${props.label} has an invalid format.`;
    return false;
  }

  if (props.type === 'number') {
    const numericValue = typeof value === 'number' ? value : Number(stringValue);
    if (Number.isNaN(numericValue)) {
      error.value = `${props.label} must be a number.`;
      return false;
    }
    if (props.min !== undefined && numericValue < props.min) {
      error.value = `${props.label} must be at least ${props.min}.`;
      return false;
    }
    if (props.max !== undefined && numericValue > props.max) {
      error.value = `${props.label} must be at most ${props.max}.`;
      return false;
    }
  }

  if (props.additionalValidator) {
    const additionalError = props.additionalValidator();
    if (additionalError) {
      error.value = additionalError;
      return false;
    }
  }

  return true;
}

defineExpose({
  validate,
})
</script>

<template>
  <UFormField :autofocus="autofocus"
              :error="error"
              :help="description"
              :hint="hint"
              :label="label"
              :required="required">
    <UInput v-model="model"
            :placeholder="placeholder"
            :type="type"
            class="w-full"/>
  </UFormField>
</template>

<style scoped>

</style>
