<script lang="ts" setup>
import AppInput from "~/components/_atoms/ui/app-input.vue";

const props = defineProps<{
  /** When set, the field is schema-driven: UForm owns validation and shows the error for this path. */
  name?: string;
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
  autocomplete?: string;
  inputmode?: 'none' | 'text' | 'tel' | 'url' | 'email' | 'numeric' | 'decimal' | 'search';
}>();

const model = defineModel<string | number>();
const error = ref<string | undefined>(undefined);
const touched = ref(false);
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
  const empty = isEmpty(value);

  if (props.required && empty) {
    error.value = t('common.validation.required', {field});
    return false;
  }

  if (empty) return true;

  const stringValue = String(value);

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
    const numericValue = Number(value);
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

  const additionalError = props.additionalValidator?.();
  if (additionalError) {
    error.value = additionalError;
    return false;
  }

  return true;
}

// Validate once the field has been left, then keep re-validating on input so the error clears
// as soon as the value becomes valid — without nagging before the user has interacted.
function onBlur() {
  if (props.name) return; // schema-driven: UForm owns validation
  touched.value = true;
  validate();
}

watch(model, () => {
  if (touched.value && error.value) validate();
});

defineExpose({
  validate,
})
</script>

<template>
  <UFormField :name="name"
              :autofocus="autofocus"
              :error="error"
              :help="description"
              :hint="hint"
              :label="label"
              :required="required">
    <AppInput v-model="model"
            :disabled="disabled"
            :placeholder="placeholder"
            :type="type"
            :autocomplete="autocomplete"
            :inputmode="inputmode"
            :ui="trailingText ? { trailing: 'pe-2' } : undefined"
            class="w-full"
            @blur="onBlur">
      <template v-if="trailingText" #trailing>
        <span class="text-xs font-medium text-muted">
          {{ trailingText }}
        </span>
      </template>
    </AppInput>
  </UFormField>
</template>
