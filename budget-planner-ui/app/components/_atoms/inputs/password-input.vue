<script lang="ts" setup>
const props = defineProps<{
  required?: boolean;
  label?: string;
  placeholder?: string;
  description?: string;
  additionalValidation?: () => boolean;
  additionalValidationMessage?: string;
}>();

const show = ref<boolean>(false)
const error = ref<string | undefined>(undefined)
const password = defineModel<string>({required: true});

function resetValidation(): void {
  error.value = undefined;
}

function validate(): boolean {
  resetValidation();

  if (props.required && !password.value) {
    error.value = `${props.label} is required.`;
    return false;
  }

  if (props.additionalValidation && !props.additionalValidation()) {
    error.value = props.additionalValidationMessage ?? 'Unknown error occurred.';
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
      :placeholder="placeholder"
      :type="show ? 'text' : 'password'"
      :ui="{ trailing: 'pe-1' }"
      class="w-full"
    >
      <template #trailing>
        <UButton
          :aria-label="show ? 'Hide password' : 'Show password'"
          :aria-pressed="show"
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
