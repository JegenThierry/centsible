<script lang="ts" setup>
import AppInput from "~/components/_atoms/ui/app-input.vue";

const props = defineProps<{
  /** When set, the field is schema-driven: UForm owns validation and shows the error for this path. */
  name?: string;
  required?: boolean;
  disabled?: boolean;
  label?: string;
  placeholder?: string;
  description?: string;
  additionalValidation?: () => boolean;
  additionalValidationMessage?: string;
  autocomplete?: string;
}>();

const password = defineModel<string>({required: true});
</script>

<template>
  <UFormField :name="name"
              :help="description"
              :label="label"
              :required="required">
    <AppInput
      v-model="password"
      :disabled="disabled"
      :placeholder="placeholder"
      :type="'password'"
      :autocomplete="autocomplete"
      :ui="{ trailing: 'pe-1' }"
      class="w-full"/>
  </UFormField>
</template>
