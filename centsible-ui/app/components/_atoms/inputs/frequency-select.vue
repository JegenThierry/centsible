<script lang="ts" setup>
import {Frequency} from "~/models/recurring/recurring-transaction";
import AppSelect from "~/components/_atoms/ui/app-select.vue";

const props = defineProps<{
  name?: string;
  label: string;
  description?: string;
  hint?: string;
  required?: boolean;
  disabled?: boolean;
}>();

const model = defineModel<Frequency>();
const {t} = useI18n();

const options = computed(() => Object.values(Frequency).map(value => ({
  label: t(`transactions.recurring.frequency.${value}`),
  value,
})));
</script>

<template>
  <UFormField :name="name"
              :help="description"
              :hint="hint"
              :label="label"
              :required="required">
    <AppSelect v-model="model"
             :disabled="disabled"
             :items="options"
             class="w-full"
             :placeholder="t('transactions.selects.selectFrequency')"
             value-key="value"/>
  </UFormField>
</template>
