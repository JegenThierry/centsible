<script lang="ts" setup>
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
  <UFormField :error="error"
              :help="description"
              :hint="hint"
              :label="label"
              :required="required">
    <UInput v-model="model"
            class="w-full"
            type="date"/>
  </UFormField>
</template>
