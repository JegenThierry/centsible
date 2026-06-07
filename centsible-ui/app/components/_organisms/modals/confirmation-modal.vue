<script lang="ts" setup>
import adze from 'adze'
import {useToasts} from "~/services/toasts/toast-service";
import ModalFooterActions from "~/components/_molecules/modals/modal-footer-actions.vue";

const props = withDefaults(defineProps<{
  /** Used by the default copy ("delete this {entity}") and the auto-managed toasts. */
  entity?: string;
  deleteCallback: () => Promise<void>;
  /** Optional overrides for non-delete or bespoke confirmations. */
  title?: string;
  body?: string;
  confirmLabel?: string;
  confirmColor?: 'primary' | 'error' | 'success' | 'warning' | 'info' | 'neutral';
  /** When false, the caller's deleteCallback owns success/error feedback. */
  manageToasts?: boolean;
}>(), {
  entity: '',
  confirmColor: 'error',
  manageToasts: true,
});

const toasts = useToasts();
const {t} = useI18n();
const isOpen = defineModel<boolean>('open', {required: true});

const loading = ref(false);

const titleText = computed(() => props.title ?? t('common.confirmDelete.title'));
const bodyText = computed(() => props.body ?? t('common.confirmDelete.question', {entity: props.entity}));
const confirmText = computed(() => props.confirmLabel ?? t('common.actions.delete'));

async function onConfirm() {
  loading.value = true;
  try {
    await props.deleteCallback();
    isOpen.value = false;
    if (props.manageToasts) {
      toasts.success(t('common.confirmDelete.successTitle', {entity: props.entity}), t('common.confirmDelete.successBody', {entity: props.entity}));
    }
  } catch (error) {
    if (props.manageToasts) {
      toasts.error(t('common.confirmDelete.errorTitle', {entity: props.entity}), t('common.confirmDelete.errorBody', {entity: props.entity}));
    }
    adze.ns('modals').error('Confirmation action failed', error);
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <UModal v-model:open="isOpen" :title="titleText">
    <template #body>
      <p>{{ bodyText }}</p>
      <p v-if="!body" class="text-sm text-muted mt-2">
        {{ t('common.confirmDelete.irreversible') }}
      </p>
    </template>

    <template #footer>
      <ModalFooterActions :loading="loading"
                          :submit-label="confirmText"
                          :submit-color="confirmColor"
                          @cancel="isOpen = false"
                          @submit="onConfirm"/>
    </template>
  </UModal>
</template>
