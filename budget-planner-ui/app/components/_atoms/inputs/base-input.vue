<script setup lang="ts">
import type {AcceptableValue} from "@nuxt/ui/runtime/types";

const props = defineProps<{
  label: string;
  description?: string;
  hint?: string;
  type: 'email' | 'number' | 'text';
  required?: boolean;
  placeholder?: string;
  additionalValidator?: () => string;
}>();

const model = defineModel<AcceptableValue>();
const error = ref<string | undefined>(undefined);

function resetValidation(): void {
  error.value = undefined;
}

function validate(): boolean {
  resetValidation();

  if (props.required) {
    error.value = `${props.label} is required.`;
    return false;
  }
  return true;
}

defineExpose({
  validate,
})
</script>

<template>
  <UFormField :required="required"
              :label="label"
              :help="description"
              :error="error"
              :hint="hint">
    <UInput v-model="model" :type="type" :placeholder="placeholder"/>
  </UFormField>
</template>

<style scoped>

</style>