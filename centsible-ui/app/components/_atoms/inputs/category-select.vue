<script lang="ts" setup>
import type {Category} from "~/models/category/category";
import type {SelectMenuItem} from "@nuxt/ui";

const props = defineProps<{
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

const items = computed<SelectMenuItem[]>(() => props.options as unknown as SelectMenuItem[]);
const selected = computed<SelectMenuItem | undefined>({
  get: () => model.value as SelectMenuItem | undefined,
  set: (value) => (model.value = value as Category | undefined),
});

const asCategory = (item: SelectMenuItem) => item as unknown as Category;
</script>

<template>
  <UFormField :name="name"
              :help="description"
              :hint="hint"
              :label="label"
              :required="required">
    <USelectMenu
      v-model="selected"
      :disabled="disabled"
      :items="items"
      class="w-full"
      label-key="name"
      :placeholder="t('transactions.selects.selectCategory')"
      searchable
    >
      <template #default="{ modelValue }">
        <div v-if="modelValue" class="flex items-center gap-2">
          <UIcon :name="asCategory(modelValue).icon" :style="{ color: asCategory(modelValue).color }" class="w-4 h-4"/>
          <span>{{ asCategory(modelValue).name }}</span>
        </div>
        <span v-else class="text-neutral-500">{{ t('transactions.selects.selectCategory') }}</span>
      </template>

      <template #item-leading="{ item: category }">
        <UIcon :name="asCategory(category).icon" :style="{ color: asCategory(category).color }" class="w-4 h-4 flex my-auto"/>
      </template>
    </USelectMenu>
  </UFormField>
</template>
