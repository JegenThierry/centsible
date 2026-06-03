<script lang="ts" setup>
import type {ConfigField, SelectOption} from "~/models/integrations/provider-descriptor";
import PasswordInput from "~/components/_atoms/inputs/password-input.vue";
import AppButton from "~/components/_atoms/ui/app-button.vue";
import AppInput from "~/components/_atoms/ui/app-input.vue";
import AppSelectMenu from "~/components/_atoms/ui/app-select-menu.vue";
import AppTextarea from "~/components/_atoms/ui/app-textarea.vue";
import AppCheckbox from "~/components/_atoms/ui/app-checkbox.vue";

const props = defineProps<{
  fields: ConfigField[];
  providerKey?: string;
  oauthLaunch?: () => void | Promise<void>;
}>();

const values = defineModel<Record<string, unknown>>({required: true});

const providersStore = useProvidersStore();
const {t} = useI18n();

const errors = ref<Record<string, string>>({});
const remoteOptions = ref<Record<string, SelectOption[]>>({});
const remotePending = ref<Record<string, boolean>>({});
const remoteQueries = ref<Record<string, string>>({});
const debounceTimers = new Map<string, ReturnType<typeof setTimeout>>();

function isEmpty(raw: unknown): boolean {
  if (raw == null) return true;
  if (typeof raw === 'string') return raw.trim().length === 0;
  return false;
}

function validate(): boolean {
  errors.value = {};
  for (const field of props.fields) {
    if (field.type === 'OAUTH_LAUNCH') continue;
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

async function loadRemoteOptions(field: ConfigField, query: string = ''): Promise<void> {
  if (!props.providerKey) return;
  if (field.type !== 'SELECT_REMOTE') return;
  // If dependencies are missing, skip the call — the dropdown shows an empty list with a hint.
  for (const dep of field.dependsOn ?? []) {
    if (isEmpty(values.value[dep])) {
      remoteOptions.value[field.name] = [];
      return;
    }
  }
  remotePending.value[field.name] = true;
  try {
    const options = await providersStore.searchProviderOptions(
      props.providerKey,
      field.name,
      query,
      {...values.value},
    );
    remoteOptions.value[field.name] = options;
  } finally {
    remotePending.value[field.name] = false;
  }
}

function onRemoteQuery(field: ConfigField, query: string) {
  remoteQueries.value[field.name] = query;
  const existing = debounceTimers.get(field.name);
  if (existing) clearTimeout(existing);
  debounceTimers.set(field.name, setTimeout(() => loadRemoteOptions(field, query), 300));
}

// Snapshot of every dependency value across all SELECT_REMOTE fields, joined into a single
// string. Watching this getter gives Vue genuinely distinct prev/next values to compare —
// `watch` on the model ref with `{deep: true}` does NOT (Vue passes the same proxy as both
// arguments on nested mutations), so the previous implementation never detected dep changes.
const dependencySnapshot = computed(() => {
  return props.fields
    .filter(f => f.type === 'SELECT_REMOTE')
    .flatMap(f => (f.dependsOn ?? []).map(d => `${f.name}:${d}=${String(values.value?.[d] ?? '')}`))
    .join('|');
});

watch(dependencySnapshot, () => {
  for (const field of props.fields) {
    if (field.type !== 'SELECT_REMOTE') continue;
    if ((field.dependsOn ?? []).length === 0) continue;
    values.value[field.name] = '';
    loadRemoteOptions(field, remoteQueries.value[field.name] ?? '');
  }
});

onMounted(() => {
  for (const field of props.fields) {
    if (field.type === 'SELECT_REMOTE' && (field.dependsOn ?? []).length === 0) {
      loadRemoteOptions(field);
    }
  }
});

onBeforeUnmount(() => {
  // Pending debounce callbacks would otherwise mutate refs (and call into the Pinia store)
  // after the component is gone, producing Vue warnings or zombie network calls.
  for (const timer of debounceTimers.values()) clearTimeout(timer);
  debounceTimers.clear();
});

defineExpose({validate});
</script>

<template>
  <div class="space-y-4">
    <template v-for="field in fields" :key="field.name">
      <UFormField v-if="field.type === 'BOOLEAN'"
                  :error="errors[field.name]"
                  :help="field.helpText"
                  :label="field.label">
        <AppCheckbox v-model="(values[field.name] as boolean)"
                   :label="field.placeholder ?? field.label"/>
      </UFormField>

      <UFormField v-else-if="field.type === 'SELECT'"
                  :error="errors[field.name]"
                  :help="field.helpText"
                  :label="field.label"
                  :required="field.required">
        <AppSelectMenu v-model="values[field.name]"
                     :items="field.options"
                     :placeholder="field.placeholder ?? t('integrations.form.selectPlaceholder')"
                     class="w-full"
                     label-key="label"
                     value-key="value"/>
      </UFormField>

      <UFormField v-else-if="field.type === 'SELECT_REMOTE'"
                  :error="errors[field.name]"
                  :help="field.helpText"
                  :label="field.label"
                  :required="field.required">
        <AppSelectMenu v-model="values[field.name]"
                     :items="remoteOptions[field.name] ?? []"
                     :loading="!!remotePending[field.name]"
                     :placeholder="field.placeholder ?? t('integrations.form.selectPlaceholder')"
                     class="w-full"
                     label-key="label"
                     searchable
                     :search-placeholder="t('integrations.form.searchPlaceholder')"
                     value-key="value"
                     @update:search-term="(q: string) => onRemoteQuery(field, q)"/>
      </UFormField>

      <UFormField v-else-if="field.type === 'MULTILINE'"
                  :error="errors[field.name]"
                  :help="field.helpText"
                  :label="field.label"
                  :required="field.required">
        <AppTextarea v-model="(values[field.name] as string)"
                   :placeholder="field.placeholder"
                   :rows="3"
                   class="w-full"/>
      </UFormField>

      <UFormField v-else-if="field.type === 'OAUTH_LAUNCH' && oauthLaunch"
                  :help="field.helpText"
                  :label="field.label">
        <AppButton block
                 icon="i-lucide-external-link"
                 variant="soft"
                 @click="oauthLaunch?.()">
          {{ field.label }}
        </AppButton>
      </UFormField>

      <!-- OAUTH_LAUNCH with no callback: skip rendering entirely. The connect-provider modal
           handles OAuth via its footer button; rendering a disabled button here would be dead UI.
           See dynamic-config-form fix #4. -->
      <template v-else-if="field.type === 'OAUTH_LAUNCH'"/>

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
        <AppInput v-model="values[field.name]"
                :placeholder="field.placeholder"
                :type="field.type === 'NUMBER' ? 'number' : 'text'"
                class="w-full"/>
      </UFormField>
    </template>
  </div>
</template>
