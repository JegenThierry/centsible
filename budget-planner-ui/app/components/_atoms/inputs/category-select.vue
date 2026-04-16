<script lang="ts" setup>
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
  <UFormField :error="error"
              :help="description"
              :hint="hint"
              :label="label"
              :required="required">
    <USelectMenu
      v-model="model"
      :items="options"
      class="w-full"
      label-key="name"
      placeholder="Select a category"
      searchable
    >
      <template #label>
        <div v-if="model" class="flex items-center gap-2">
          <UIcon :name="model.icon" :style="{ color: model.color }" class="w-4 h-4"/>
          <span>{{ model.name }}</span>
        </div>
        <span v-else>Select a category</span>
      </template>

      <template #item-leading="{ item: category }">
        <UIcon :name="category.icon" :style="{ color: category.color }" class="w-4 h-4 flex my-auto"/>
      </template>
    </USelectMenu>
  </UFormField>
</template>
