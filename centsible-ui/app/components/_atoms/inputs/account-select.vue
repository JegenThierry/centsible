<script lang="ts" setup>
import type {BudgetAccount} from "~/models/budget-account/budget-account";

const props = defineProps<{
  label: string;
  description?: string;
  hint?: string;
  required?: boolean;
  disabled?: boolean;
  options: BudgetAccount[];
}>();

const model = defineModel<BudgetAccount | undefined>();
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

defineExpose({validate})
</script>

<template>
  <UFormField :error="error"
              :help="description"
              :hint="hint"
              :label="label"
              :required="required">
    <USelectMenu v-model="model"
                 :disabled="disabled"
                 :items="options"
                 class="w-full"
                 label-key="name"
                 :placeholder="t('transactions.selects.selectAccount')"
                 searchable>
      <template #default="{ modelValue }">
        <div v-if="modelValue" class="flex items-center gap-2">
          <UIcon class="w-4 h-4" name="i-lucide-wallet"/>
          <span>{{ modelValue.name }}</span>
          <span class="text-xs text-neutral-500">({{ modelValue.currency }})</span>
        </div>
        <span v-else class="text-neutral-500">{{ t('transactions.selects.selectAccount') }}</span>
      </template>

      <template #item-leading="{ item }">
        <UIcon class="w-4 h-4" name="i-lucide-wallet"/>
      </template>
    </USelectMenu>
  </UFormField>
</template>
