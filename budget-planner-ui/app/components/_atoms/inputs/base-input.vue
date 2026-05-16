<script lang="ts" setup>
const props = defineProps<{
  label: string;
  description?: string;
  hint?: string;
  type: 'email' | 'number' | 'text';
  required?: boolean;
  disabled?: boolean;
  placeholder?: string;
  additionalValidator?: () => string | undefined;
  autofocus?: boolean;
  min?: number;
  max?: number;
  minLength?: number;
  maxLength?: number;
  pattern?: RegExp;
  patternMessage?: string;
  trailingText?: string;
}>();

const model = defineModel<string | number>();
const error = ref<string | undefined>(undefined);
const {t} = useI18n();

function isEmpty(value: string | number | undefined): boolean {
  if (value === undefined || value === null) return true;
  if (typeof value === 'string') return value.trim().length === 0;
  return false;
}

function validate(): boolean {
  error.value = undefined;

  const value = model.value;
  const field = props.label;

  if (props.required && isEmpty(value)) {
    error.value = t('common.validation.required', {field});
    return false;
  }

  if (isEmpty(value)) {
    return true;
  }

  const stringValue = typeof value === 'string' ? value : String(value);

  if (props.type === 'email' && !EMAIL_REGEX.test(stringValue.trim())) {
    error.value = t('common.validation.email', {field});
    return false;
  }

  if (props.minLength !== undefined && stringValue.length < props.minLength) {
    error.value = t('common.validation.minLength', {field, min: props.minLength});
    return false;
  }

  if (props.maxLength !== undefined && stringValue.length > props.maxLength) {
    error.value = t('common.validation.maxLength', {field, max: props.maxLength});
    return false;
  }

  if (props.pattern && !props.pattern.test(stringValue)) {
    error.value = props.patternMessage ?? t('common.validation.invalidFormat', {field});
    return false;
  }

  if (props.type === 'number') {
    const numericValue = typeof value === 'number' ? value : Number(stringValue);
    if (Number.isNaN(numericValue)) {
      error.value = t('common.validation.number', {field});
      return false;
    }
    if (props.min !== undefined && numericValue < props.min) {
      error.value = t('common.validation.min', {field, min: props.min});
      return false;
    }
    if (props.max !== undefined && numericValue > props.max) {
      error.value = t('common.validation.max', {field, max: props.max});
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
            :disabled="disabled"
            :placeholder="placeholder"
            :type="type"
            :ui="trailingText ? { trailing: 'pe-2' } : undefined"
            class="w-full">
      <template v-if="trailingText" #trailing>
        <span class="text-xs font-medium text-neutral-500 dark:text-neutral-400">
          {{ trailingText }}
        </span>
      </template>
    </UInput>
  </UFormField>
</template>

<style scoped>

</style>
