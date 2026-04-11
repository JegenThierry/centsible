<script setup lang="ts">
import type {Category} from "~/models/category/category";

const props = defineProps<{
  label: string;
  description?: string;
  hint?: string;
  required?: boolean;
  options: Category[];
}>();

const model = defineModel<Category | undefined>();
const error = ref<string | undefined>(undefined);

function validate(): boolean {
  error.value = undefined;
  if (props.required && !model.value) {
    error.value = `${props.label} is required.`;
    return false;
  }
  return true;
}

defineExpose({
  validate,
})
</script>

<template>
  <UFormField :required="required"
              :label="label"
              :help="description"
              :error="error"
              :hint="hint">
    <USelectMenu
        v-model="model"
        :items="options"
        label-key="name"
        searchable
        placeholder="Select a category"
        class="w-full"
    >
      <template #label>
        <div v-if="model" class="flex items-center gap-2">
          <UIcon :name="model.icon" class="w-4 h-4" />
          <span>{{ model.name }}</span>
        </div>
        <span v-else>Select a category</span>
      </template>

      <template #item-leading="{ item: category }">
        <UIcon :name="category.icon" class="w-4 h-4" />
      </template>
    </USelectMenu>
  </UFormField>
</template>
