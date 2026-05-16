<script lang="ts" setup>
import {useProvidersStore} from "~/stores/providersStore";
import type {ProviderDescriptor} from "~/models/integrations/provider-descriptor";
import DynamicConfigForm from "~/components/_organisms/integrations/dynamic-config-form.vue";
import CancelButton from "~/components/_molecules/buttons/cancel-button.vue";

const props = defineProps<{
  descriptor?: ProviderDescriptor;
}>();

const isOpen = defineModel<boolean>('open', {required: true});

const providersStore = useProvidersStore();
const {t} = useI18n();

const displayName = ref<string>('');
const values = ref<Record<string, unknown>>({});
const displayNameError = ref<string | undefined>();
const formRef = ref<InstanceType<typeof DynamicConfigForm>>();
const loading = ref(false);

const modalTitle = computed(() => {
  if (props.descriptor) {
    return t('integrations.modal.titleWithName', {name: props.descriptor.displayName});
  }
  return t('integrations.modal.titleFallback');
});

const modalDescription = computed(() => {
  return props.descriptor?.description ?? t('integrations.modal.descriptionFallback');
});

function reset() {
  displayName.value = props.descriptor?.displayName ?? '';
  values.value = {};
  displayNameError.value = undefined;
}

watch(isOpen, (open) => {
  if (open) reset();
});

async function handleSave() {
  if (!props.descriptor) return;
  displayNameError.value = displayName.value.trim().length === 0
    ? t('integrations.modal.displayNameRequired')
    : undefined;
  const configValid = formRef.value?.validate() ?? true;
  if (displayNameError.value || !configValid) return;

  loading.value = true;
  try {
    await providersStore.createConnection({
      providerKey: props.descriptor.key,
      displayName: displayName.value.trim(),
      values: values.value,
    });
    isOpen.value = false;
  } catch {
    // toast handled by store
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <UModal v-model:open="isOpen"
          :description="modalDescription"
          :title="modalTitle">
    <template #body>
      <div v-if="descriptor" class="space-y-4">
        <UFormField :error="displayNameError"
                    :help="t('integrations.modal.displayNameHelp')"
                    :label="t('integrations.modal.displayNameLabel')"
                    required>
          <UInput v-model="displayName"
                  :placeholder="descriptor.displayName"
                  class="w-full"/>
        </UFormField>

        <DynamicConfigForm ref="formRef"
                           v-model="values"
                           :fields="descriptor.configFields"/>
      </div>
    </template>

    <template #footer>
      <div class="flex justify-end gap-2">
        <CancelButton @click="isOpen = false"/>
        <UButton :loading="loading" @click="handleSave">{{ t('integrations.modal.submit') }}</UButton>
      </div>
    </template>
  </UModal>
</template>
