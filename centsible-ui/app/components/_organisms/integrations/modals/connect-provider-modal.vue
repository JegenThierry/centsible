<script lang="ts" setup>
import adze from 'adze'
import {useProvidersStore} from "~/stores/providersStore";
import type {ProviderDescriptor} from "~/models/integrations/provider-descriptor";
import DynamicConfigForm from "~/components/_organisms/integrations/dynamic-config-form.vue";
import ModalFooterActions from "~/components/_molecules/modals/modal-footer-actions.vue";

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
const createdConnectionId = ref<string | undefined>();

const isOAuth = computed(() => props.descriptor?.authType === 'OAUTH2');

const modalTitle = computed(() => props.descriptor
  ? t('integrations.modal.titleWithName', {name: props.descriptor.displayName})
  : t('integrations.modal.titleFallback'));

const modalDescription = computed(() =>
  props.descriptor?.description ?? t('integrations.modal.descriptionFallback'));

const submitLabel = computed(() => {
  if (isOAuth.value) {
    return createdConnectionId.value
      ? t('integrations.modal.continueToProvider', {name: props.descriptor?.displayName ?? ''})
      : t('integrations.modal.saveAndContinue');
  }
  return t('integrations.modal.submit');
});

function reset() {
  displayName.value = props.descriptor?.displayName ?? '';
  values.value = {};
  displayNameError.value = undefined;
  createdConnectionId.value = undefined;
}

watch(isOpen, (open) => {
  if (open) reset();
});

async function handleSave() {
  if (!props.descriptor) return;

  // Step 2 of the OAuth flow: connection already created; redirect to provider.
  if (isOAuth.value && createdConnectionId.value) {
    await launchOAuth(createdConnectionId.value);
    return;
  }

  displayNameError.value = displayName.value.trim().length === 0
    ? t('integrations.modal.displayNameRequired')
    : undefined;
  if (displayNameError.value) return;
  if (!formRef.value?.validate()) return;

  loading.value = true;
  try {
    const created = await providersStore.createConnection({
      providerKey: props.descriptor.key,
      displayName: displayName.value.trim(),
      values: values.value,
    });
    if (isOAuth.value && created) {
      // Stay open so the user sees the "Continue to {provider}" CTA.
      createdConnectionId.value = created.id;
    } else {
      isOpen.value = false;
    }
  } catch (error) {
    adze.ns('integrations').error('Create provider connection failed', error);
  } finally {
    loading.value = false;
  }
}

async function launchOAuth(connectionId: string) {
  loading.value = true;
  try {
    const url = await providersStore.startOAuth(connectionId);
    if (url) {
      window.location.href = url;
    }
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
        <UAlert v-if="isOAuth && !createdConnectionId"
                color="info"
                icon="i-lucide-info"
                :title="t('integrations.modal.oauthStep1Title')"
                :description="t('integrations.modal.oauthStep1Description')"
                variant="soft"/>
        <UAlert v-else-if="isOAuth && createdConnectionId"
                color="success"
                icon="i-lucide-check-circle-2"
                :title="t('integrations.modal.oauthStep2Title')"
                :description="t('integrations.modal.oauthStep2Description', {name: descriptor.displayName})"
                variant="soft"/>

        <UFormField :error="displayNameError"
                    :help="t('integrations.modal.displayNameHelp')"
                    :label="t('integrations.modal.displayNameLabel')"
                    required>
          <UInput v-model="displayName"
                  :disabled="!!createdConnectionId"
                  :placeholder="descriptor.displayName"
                  class="w-full"/>
        </UFormField>

        <DynamicConfigForm ref="formRef"
                           v-model="values"
                           :fields="descriptor.configFields"
                           :provider-key="descriptor.key"/>
      </div>
    </template>

    <template #footer>
      <ModalFooterActions :loading="loading"
                          :submit-icon="isOAuth && createdConnectionId ? 'i-lucide-external-link' : 'i-lucide-check'"
                          :submit-label="submitLabel"
                          @cancel="isOpen = false"
                          @submit="handleSave"/>
    </template>
  </UModal>
</template>
