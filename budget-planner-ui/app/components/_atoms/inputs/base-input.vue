<script lang="ts" setup>
const props = defineProps<{
  label: string;
  description?: string;
  hint?: string;
  type: 'email' | 'number' | 'text';
  required?: boolean;
  placeholder?: string;
  additionalValidator?: () => string;
  autofocus?: boolean;
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
