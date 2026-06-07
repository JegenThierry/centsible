<script lang="ts" setup>
import AppInput from "~/components/_atoms/ui/app-input.vue";

const props = defineProps<{
  name?: string;
  label: string;
  description?: string;
  hint?: string;
  required?: boolean;
  disabled?: boolean;
}>();

const model = defineModel<string | undefined>();
const {t} = useI18n();
</script>

<template>
  <UFormField :name="name"
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
