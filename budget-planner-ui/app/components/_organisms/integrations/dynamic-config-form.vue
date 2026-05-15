<script lang="ts" setup>
import type {ConfigField} from "~/models/integrations/provider-descriptor";

const props = defineProps<{
  fields: ConfigField[];
}>();

const values = defineModel<Record<string, unknown>>({required: true});

const errors = ref<Record<string, string>>({});
const showSecret = ref<Record<string, boolean>>({});

function toggleSecret(name: string) {
  showSecret.value[name] = !showSecret.value[name];
}

function isEmpty(raw: unknown): boolean {
  if (raw == null) return true;
  if (typeof raw === 'string') return raw.trim().length === 0;
  return false;
}

function validate(): boolean {
  errors.value = {};
  let ok = true;
  for (const field of props.fields) {
    const raw = values.value[field.name];
    if (field.required && field.type !== 'BOOLEAN' && isEmpty(raw)) {
      errors.value[field.name] = `${field.label} is required.`;
      ok = false;
      continue;
    }
    if (field.type === 'NUMBER' && !isEmpty(raw) && Number.isNaN(Number(raw))) {
      errors.value[field.name] = `${field.label} must be a number.`;
      ok = false;
    }
    if (field.type === 'SELECT' && !isEmpty(raw) && field.options.length > 0) {
      const allowed = field.options.map(o => o.value);
      if (!allowed.includes(String(raw))) {
        errors.value[field.name] = `${field.label} must be one of: ${allowed.join(', ')}.`;
        ok = false;
      }
    }
  }
  return ok;
}

defineExpose({validate});
</script>

<template>
  <div class="space-y-4">
    <template v-for="field in fields" :key="field.name">
      <!-- Boolean -->
      <UFormField v-if="field.type === 'BOOLEAN'"
                  :error="errors[field.name]"
                  :help="field.helpText"
                  :label="field.label">
        <UCheckbox v-model="(values[field.name] as boolean)"
                   :label="field.placeholder ?? field.label"/>
      </UFormField>

      <!-- Select -->
      <UFormField v-else-if="field.type === 'SELECT'"
                  :error="errors[field.name]"
                  :help="field.helpText"
                  :label="field.label"
                  :required="field.required">
        <USelectMenu v-model="values[field.name]"
                     :items="field.options"
                     :placeholder="field.placeholder ?? 'Select…'"
                     class="w-full"
                     label-key="label"
                     value-key="value"/>
      </UFormField>

      <!-- Multiline -->
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

      <!-- Secret (mirrors password-input pattern) -->
      <UFormField v-else-if="field.secret"
                  :error="errors[field.name]"
                  :help="field.helpText"
                  :label="field.label"
                  :required="field.required">
        <UInput v-model="(values[field.name] as string)"
                :placeholder="field.placeholder"
                :type="showSecret[field.name] ? 'text' : 'password'"
                :ui="{ trailing: 'pe-1' }"
                class="w-full">
          <template #trailing>
            <UButton :aria-label="showSecret[field.name] ? 'Hide value' : 'Show value'"
                     :icon="showSecret[field.name] ? 'i-lucide-eye-off' : 'i-lucide-eye'"
                     color="neutral"
                     size="sm"
                     variant="link"
                     @click="toggleSecret(field.name)"/>
          </template>
        </UInput>
      </UFormField>

      <!-- STRING / NUMBER -->
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
