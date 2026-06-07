<script lang="ts" setup>
import AppInput from "~/components/_atoms/ui/app-input.vue";

defineProps<{
  /** Form-state path this field maps to; UForm shows the schema error for this name. */
  name?: string;
  label: string;
  description?: string;
  hint?: string;
  type: 'email' | 'number' | 'text';
  required?: boolean;
  disabled?: boolean;
  placeholder?: string;
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
</script>

<template>
  <UFormField :name="name"
              :autofocus="autofocus"
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
            class="w-full">
      <template v-if="trailingText" #trailing>
        <span class="text-xs font-medium text-muted">
          {{ trailingText }}
        </span>
      </template>
    </AppInput>
  </UFormField>
</template>
