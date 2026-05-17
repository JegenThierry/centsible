<script lang="ts" setup>
import type {ConfigField} from "~/models/integrations/provider-descriptor";
import PasswordInput from "~/components/_atoms/inputs/password-input.vue";

const props = defineProps<{
  fields: ConfigField[];
}>();

const values = defineModel<Record<string, unknown>>({required: true});

const {t} = useI18n();

const errors = ref<Record<string, string>>({});

function isEmpty(raw: unknown): boolean {
  if (raw == null) return true;
  if (typeof raw === 'string') return raw.trim().length === 0;
  return false;
}

function validate(): boolean {
  errors.value = {};
  for (const field of props.fields) {
    const raw = values.value[field.name];
    if (field.required && field.type !== 'BOOLEAN' && isEmpty(raw)) {
      errors.value[field.name] = t('integrations.form.errors.required', {field: field.label});
      continue;
    }
    if (isEmpty(raw)) continue;
    if (field.type === 'NUMBER' && Number.isNaN(Number(raw))) {
      errors.value[field.name] = t('integrations.form.errors.number', {field: field.label});
      continue;
    }
    if (field.type === 'SELECT' && field.options.length > 0) {
      const allowed = field.options.map(o => o.value);
      if (!allowed.includes(String(raw))) {
        errors.value[field.name] = t('integrations.form.errors.select', {field: field.label, options: allowed.join(', ')});
      }
    }
  }
  return Object.keys(errors.value).length === 0;
}

defineExpose({validate});
</script>

<template>
  <div class="space-y-4">
    <template v-for="field in fields" :key="field.name">
      <UFormField v-if="field.type === 'BOOLEAN'"
                  :error="errors[field.name]"
                  :help="field.helpText"
                  :label="field.label">
        <UCheckbox v-model="(values[field.name] as boolean)"
                   :label="field.placeholder ?? field.label"/>
      </UFormField>

      <UFormField v-else-if="field.type === 'SELECT'"
                  :error="errors[field.name]"
                  :help="field.helpText"
                  :label="field.label"
                  :required="field.required">
        <USelectMenu v-model="values[field.name]"
                     :items="field.options"
                     :placeholder="field.placeholder ?? t('integrations.form.selectPlaceholder')"
                     class="w-full"
                     label-key="label"
                     value-key="value"/>
      </UFormField>

      <UFormField v-else-if="field.type === 'MULTILINE'"
                  :error="errors[field.name]"
                  :help="field.helpText"
                  :label="field.label"
                  :required="field.required">
        <UTextarea v-model="(values[field.name] as string)"
                   :placeholder="field.placeholder"
                   :rows="3"
                   class="w-full"/>
      </UFormField>

      <PasswordInput v-else-if="field.secret"
                     v-model="(values[field.name] as string)"
                     :description="field.helpText"
                     :label="field.label"
                     :placeholder="field.placeholder"
                     :required="field.required"/>

      <UFormField v-else
                  :error="errors[field.name]"
                  :help="field.helpText"
                  :label="field.label"
                  :required="field.required">
        <UInput v-model="values[field.name]"
                :placeholder="field.placeholder"
                :type="field.type === 'NUMBER' ? 'number' : 'text'"
                class="w-full"/>
      </UFormField>
    </template>
  </div>
</template>
