<script lang="ts" setup>
import type {BudgetAccount} from "~/models/budget-account/budget-account";

const props = defineProps<{
  label: string;
  description?: string;
  hint?: string;
  required?: boolean;
  options: BudgetAccount[];
}>();

const model = defineModel<BudgetAccount | undefined>();
const error = ref<string | undefined>(undefined);

function validate(): boolean {
  error.value = undefined;
  if (props.required && !model.value) {
    error.value = `${props.label} is required.`;
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
                 :items="options"
                 class="w-full"
                 label-key="name"
                 placeholder="Select an account"
                 searchable>
      <template #label>
        <div v-if="model" class="flex items-center gap-2">
          <UIcon class="w-4 h-4" name="i-lucide-wallet"/>
          <span>{{ model.name }}</span>
          <span class="text-xs text-neutral-500">({{ model.currency }})</span>
        </div>
        <span v-else>Select an account</span>
      </template>

      <template #item-leading="{ item }">
        <UIcon class="w-4 h-4" name="i-lucide-wallet"/>
      </template>
    </USelectMenu>
  </UFormField>
</template>
