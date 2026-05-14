<script lang="ts" setup>
import BaseInput from "~/components/_atoms/inputs/base-input.vue";

const model = defineModel<string>({required: true});

defineProps<{
  label?: string;
  required?: boolean;
  placeholder?: string;
}>();

const iconInputRef = ref<InstanceType<typeof BaseInput>>();

function validate() {
  return iconInputRef.value?.validate() ?? true;
}

defineExpose({
  validate,
});
</script>

<template>
  <div class="space-y-1">
    <div class="flex items-end gap-2">
      <div class="flex-1">
        <BaseInput
          ref="iconInputRef"
          v-model="model"
          :label="label || 'Icon (Lucide name)'"
          :max-length="50"
          :pattern="ICON_PATTERN"
          :placeholder="placeholder || 'i-lucide-tag'"
          :required="required"
          pattern-message="Icon must be a Lucide name like 'i-lucide-tag'."
          type="text"
        />
      </div>
      <div
        class="p-2 border rounded-md dark:border-neutral-700 bg-neutral-50 dark:bg-neutral-800 flex items-center justify-center">
        <UIcon
          :name="model || 'i-lucide-help-circle'"
          class="w-4 h-4"
        />
      </div>
    </div>
    <p class="text-xs text-neutral-500">
      Find icons at <a class="text-primary-500 underline" href="https://lucide.dev/icons"
                       target="_blank">lucide.dev</a>. Use <code>i-lucide-[name]</code> format.
    </p>
  </div>
</template>
