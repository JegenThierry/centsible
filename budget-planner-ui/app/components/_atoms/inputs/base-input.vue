<script setup lang="ts">
const props = defineProps<{
  label: string;
  description?: string;
  hint?: string;
  type: 'email' | 'number' | 'text';
  required?: boolean;
  placeholder?: string;
  additionalValidator?: () => string;
}>();

const model = defineModel<string | number>();
const error = ref<string | undefined>(undefined);

function resetValidation(): void {
  error.value = undefined;
}

function validate(): boolean {
  resetValidation();

  if (props.required && !model.value) {
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
    <UInput class="w-full"
            v-model="model"
            :type="type"
            :placeholder="placeholder"/>
  </UFormField>
</template>

<style scoped>

</style>