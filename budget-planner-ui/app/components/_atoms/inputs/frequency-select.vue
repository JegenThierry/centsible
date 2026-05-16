<script lang="ts" setup>
import {Frequency} from "~/models/recurring/recurring-transaction";

const props = defineProps<{
  label: string;
  description?: string;
  hint?: string;
  required?: boolean;
}>();

const model = defineModel<Frequency>();
const error = ref<string | undefined>(undefined);
const {t} = useI18n();

const options = computed(() => Object.values(Frequency).map(value => ({
  label: t(`transactions.recurring.frequency.${value}`),
  value,
})));

function validate(): boolean {
  error.value = undefined;
  if (props.required && !model.value) {
    error.value = t('common.validation.required', {field: props.label});
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
             :placeholder="t('transactions.selects.selectFrequency')"
             value-key="value"/>
  </UFormField>
</template>
