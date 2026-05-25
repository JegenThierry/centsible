<script lang="ts" setup>
import type {Contact} from "~/models/contact/contact";

const props = defineProps<{
  label: string;
  description?: string;
  hint?: string;
  required?: boolean;
  disabled?: boolean;
  options: Contact[];
}>();

const model = defineModel<Contact | undefined>();
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
                 :placeholder="t('transactions.selects.selectContact')"
                 searchable>
      <template #default="{ modelValue }">
        <UButton color="neutral" variant="outline" class="w-full justify-between">
          <div v-if="modelValue" class="flex items-center gap-2">
            <UAvatar v-if="modelValue.picture" :src="modelValue.picture" size="2xs"/>
            <UIcon v-else class="w-4 h-4" name="i-lucide-user"/>
            <span>{{ modelValue.name }}</span>
          </div>
          <span v-else class="text-neutral-500">{{ t('transactions.selects.selectContact') }}</span>
          <UIcon name="i-lucide-chevron-down" class="w-4 h-4 text-neutral-500"/>
        </UButton>
      </template>

      <template #item-leading="{ item }">
        <UAvatar v-if="item.picture" :src="item.picture" size="2xs"/>
        <UIcon v-else class="w-4 h-4" name="i-lucide-user"/>
      </template>
    </USelectMenu>
  </UFormField>
</template>
