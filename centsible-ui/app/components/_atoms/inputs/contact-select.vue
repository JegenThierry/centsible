<script lang="ts" setup>
import type {Contact} from "~/models/contact/contact";

const props = defineProps<{
  name?: string;
  label: string;
  description?: string;
  hint?: string;
  required?: boolean;
  disabled?: boolean;
  options: Contact[];
}>();

const model = defineModel<Contact | undefined>();
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
                 :placeholder="t('transactions.selects.selectContact')"
                 searchable>
      <template #default="{ modelValue }">
        <div v-if="modelValue" class="flex items-center gap-2">
          <UAvatar v-if="modelValue.picture" :src="modelValue.picture" size="2xs"/>
          <UIcon v-else class="w-4 h-4" name="i-lucide-user"/>
          <span>{{ modelValue.name }}</span>
        </div>
        <span v-else class="text-neutral-500">{{ t('transactions.selects.selectContact') }}</span>
      </template>

      <template #item-leading="{ item }">
        <UAvatar v-if="item.picture" :src="item.picture" size="2xs"/>
        <UIcon v-else class="w-4 h-4" name="i-lucide-user"/>
      </template>
    </USelectMenu>
  </UFormField>
</template>
