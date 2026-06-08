<script lang="ts" setup>
import type {Category} from "~/models/category/category";

defineProps<{
  name?: string;
  label: string;
  description?: string;
  hint?: string;
  required?: boolean;
  disabled?: boolean;
  options: Category[];
}>();

const model = defineModel<Category | undefined>();
const {t} = useI18n();
</script>

<template>
  <UFormField :name="name"
              :help="description"
              :hint="hint"
              :label="label"
              :required="required">
    <USelectMenu v-model="model"
                 :disabled="disabled"
                 :items="options"
                 class="w-full"
                 label-key="name"
                 :placeholder="t('transactions.selects.selectCategory')"
                 searchable>
      <template #default="{ modelValue }">
        <div v-if="modelValue" class="flex items-center gap-2">
          <UIcon :name="modelValue.icon" :style="{ color: modelValue.color }" class="w-4 h-4"/>
          <span>{{ modelValue.name }}</span>
        </div>
        <span v-else class="text-neutral-500">{{ t('transactions.selects.selectCategory') }}</span>
      </template>

      <template #item-leading="{ item }">
        <UIcon :name="item.icon" :style="{ color: item.color }" class="w-4 h-4 flex my-auto"/>
      </template>
    </USelectMenu>
  </UFormField>
</template>
