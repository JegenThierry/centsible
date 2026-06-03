<script lang="ts" setup>
import AppInput from "~/components/_atoms/ui/app-input.vue";

const props = defineProps<{
  label: string;
  description?: string;
  hint?: string;
  required?: boolean;
  disabled?: boolean;
}>();

const model = defineModel<string | undefined>();
const error = ref<string | undefined>(undefined);
const {t} = useI18n();

function validate(): boolean {
  error.value = undefined;
  if (props.required && !model.value) {
    error.value = t('common.validation.required', {field: props.label});
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
    <AppInput v-model="model"
            :disabled="disabled"
            class="w-full"
            type="date"/>
  </UFormField>
</template>
