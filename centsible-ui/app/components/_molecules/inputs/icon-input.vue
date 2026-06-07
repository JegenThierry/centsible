<script lang="ts" setup>
import BaseInput from "~/components/_atoms/inputs/base-input.vue";

const model = defineModel<string>({required: true});

defineProps<{
  name?: string;
  label?: string;
  required?: boolean;
  placeholder?: string;
}>();

const {t} = useI18n();

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
          :name="name"
          v-model="model"
          :label="label || t('categories.form.iconLabel')"
          :max-length="50"
          :pattern="ICON_PATTERN"
          :placeholder="placeholder || t('categories.form.iconPlaceholder')"
          :required="required"
          :pattern-message="t('categories.form.iconPatternMessage')"
          type="text"
        />
      </div>
      <div
        class="p-2 border border-default rounded-md bg-muted flex items-center justify-center">
        <UIcon
          :name="model || 'i-lucide-help-circle'"
          class="w-4 h-4"
        />
      </div>
    </div>
    <p class="text-xs text-muted">
      {{ t('categories.form.iconHelp') }}
    </p>
  </div>
</template>
