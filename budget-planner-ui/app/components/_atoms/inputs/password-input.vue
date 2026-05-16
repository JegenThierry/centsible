<script lang="ts" setup>
const props = defineProps<{
  required?: boolean;
  disabled?: boolean;
  label?: string;
  placeholder?: string;
  description?: string;
  additionalValidation?: () => boolean;
  additionalValidationMessage?: string;
}>();

const show = ref<boolean>(false)
const error = ref<string | undefined>(undefined)
const password = defineModel<string>({required: true});
const {t} = useI18n();

function resetValidation(): void {
  error.value = undefined;
}

function validate(): boolean {
  resetValidation();

  if (props.required && !password.value) {
    error.value = t('common.validation.required', {field: props.label ?? ''});
    return false;
  }

  if (props.additionalValidation && !props.additionalValidation()) {
    error.value = props.additionalValidationMessage ?? t('common.states.error');
    return false;
  }

  return true;
}

function onToggleShow(): void {
  show.value = !show.value;
}

defineExpose({
  validate,
})
</script>

<template>
  <UFormField :error="error"
              :help="description"
              :label="label"
              :required="required">
    <UInput
      v-model="password"
      :disabled="disabled"
      :placeholder="placeholder"
      :type="show ? 'text' : 'password'"
      :ui="{ trailing: 'pe-1' }"
      class="w-full"
    >
      <template #trailing>
        <UButton
          :aria-label="show ? t('auth.password.hide') : t('auth.password.show')"
          :aria-pressed="show"
          :disabled="disabled"
          :icon="show ? 'i-lucide-eye-off' : 'i-lucide-eye'"
          aria-controls="password"
          color="neutral"
          size="sm"
          variant="link"
          @click="onToggleShow()"
        />
      </template>
    </UInput>
  </UFormField>
</template>

<style scoped>

</style>
