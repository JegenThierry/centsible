<script lang="ts" setup>
import type {Category} from "~/models/category/category";

const props = defineProps<{
  label: string;
  description?: string;
  hint?: string;
  required?: boolean;
  disabled?: boolean;
  options: Category[];
}>();

const model = defineModel<Category | undefined>();
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
    <USelectMenu
      v-model="model"
      :disabled="disabled"
      :items="options"
      class="w-full"
      label-key="name"
      :placeholder="t('transactions.selects.selectCategory')"
      searchable
    >
      <template #default="{ modelValue }">
        <UButton color="neutral" variant="outline" class="w-full justify-between">
          <div v-if="modelValue" class="flex items-center gap-2">
            <UIcon :name="modelValue.icon" :style="{ color: modelValue.color }" class="w-4 h-4"/>
            <span>{{ modelValue.name }}</span>
          </div>
          <span v-else class="text-neutral-500">{{ t('transactions.selects.selectCategory') }}</span>
          <UIcon name="i-lucide-chevron-down" class="w-4 h-4 text-neutral-500"/>
        </UButton>
      </template>

      <template #item-leading="{ item: category }">
        <UIcon :name="category.icon" :style="{ color: category.color }" class="w-4 h-4 flex my-auto"/>
      </template>
    </USelectMenu>
  </UFormField>
</template>
