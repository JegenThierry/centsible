<script lang="ts" setup>
import type {Contact} from "~/models/contact/contact";

const props = defineProps<{
  label: string;
  description?: string;
  hint?: string;
  required?: boolean;
  options: Contact[];
}>();

const model = defineModel<Contact | undefined>();
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
                 placeholder="Select a contact"
                 searchable>
      <template #label>
        <div v-if="model" class="flex items-center gap-2">
          <UAvatar v-if="model.picture" :src="model.picture" size="2xs"/>
          <UIcon v-else class="w-4 h-4" name="i-lucide-user"/>
          <span>{{ model.name }}</span>
        </div>
        <span v-else>Select a contact</span>
      </template>

      <template #item-leading="{ item }">
        <UAvatar v-if="item.picture" :src="item.picture" size="2xs"/>
        <UIcon v-else class="w-4 h-4" name="i-lucide-user"/>
      </template>
    </USelectMenu>
  </UFormField>
</template>
