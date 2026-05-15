<script lang="ts" setup>
import {Frequency, FREQUENCY_LABELS} from "~/models/recurring/recurring-transaction";

const props = defineProps<{
  label: string;
  description?: string;
  hint?: string;
  required?: boolean;
}>();

const model = defineModel<Frequency>();
const error = ref<string | undefined>(undefined);

const options = Object.values(Frequency).map(value => ({
  label: FREQUENCY_LABELS[value],
  value,
}));

function validate(): boolean {
  error.value = undefined;
  if (props.required && !model.value) {
    error.value = `${props.label} is required.`;
    return false;
  }
  return true;
}

defineExpose({validate});
</script>

<template>
  <UFormField :error="error"
              :help="description"
              :hint="hint"
              :label="label"
              :required="required">
    <USelect v-model="model"
             :items="options"
             class="w-full"
             placeholder="Select a frequency"
             value-key="value"/>
  </UFormField>
</template>
