<script setup lang="ts">
const props = defineProps<{
  label: string;
  description?: string;
  hint?: string;
  required?: boolean;
}>();

const model = defineModel<string | undefined>();
const error = ref<string | undefined>(undefined);

function validate(): boolean {
  error.value = undefined;
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
            type="date"/>
  </UFormField>
</template>
