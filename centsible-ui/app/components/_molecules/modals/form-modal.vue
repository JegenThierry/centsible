<script lang="ts" setup generic="State extends Record<string, unknown>">
import {computed} from 'vue';
import type {FormSubmitEvent} from '@nuxt/ui';
import type {ZodType} from 'zod';
import {useToasts} from '~/services/toasts/toast-service';
import {useModalDirtyGuard} from '~/composables/use-unsaved-changes-guard';
import ModalFooterActions from '~/components/_molecules/modals/modal-footer-actions.vue';

const props = defineProps<{
  title: string;
  description?: string;
  schema: ZodType;
  state: State;
  submitLabel: string;
  loading?: boolean;
  /** JSON-snapshotted by the dirty guard to detect unsaved edits (see useModalDirtyGuard). */
  getSnapshot: () => unknown;
  /** Re-seeds the form each time the modal (re)opens. */
  onResetOnOpen?: () => void;
  submitColor?: 'primary' | 'error' | 'success' | 'warning' | 'info' | 'neutral';
  submitIcon?: string;
  /** When both are set, a client-side validation failure surfaces this toast. */
  validationErrorTitle?: string;
  validationErrorBody?: string;
}>();

const emit = defineEmits<{
  (e: 'submit', event: FormSubmitEvent<State>): void;
}>();

const open = defineModel<boolean>({required: true});
const loading = computed(() => props.loading ?? false);
const formId = useId();
const toasts = useToasts();

const {requestClose, captureSnapshot} = useModalDirtyGuard({
  isOpen: open,
  loading,
  getSnapshot: () => props.getSnapshot(),
  onResetOnOpen: () => props.onResetOnOpen?.(),
});

function onValidationError() {
  if (props.validationErrorTitle && props.validationErrorBody) {
    toasts.error(props.validationErrorTitle, props.validationErrorBody);
  }
}

function onSubmit(event: FormSubmitEvent<State>) {
  emit('submit', event);
}

// Exposed so a parent that loads its form asynchronously (or re-seeds on a prop change) can
// re-baseline the dirty snapshot after the data lands — otherwise the loaded, unedited form
// would read as dirty and trigger a spurious discard prompt on close.
defineExpose({requestClose, captureSnapshot});
</script>

<template>
  <UModal
    :open="open"
    :close="{
      color: 'primary',
      variant: 'outline',
      class: 'rounded-full',
    }"
    :description="description"
    :title="title"
    @update:open="requestClose"
  >
    <template #body>
      <UForm :id="formId" :schema="schema" :state="state" class="space-y-4 py-2 flex flex-col"
             @submit="onSubmit" @error="onValidationError">
        <slot name="fields"/>
      </UForm>
      <!-- Optional content rendered inside the modal body but OUTSIDE the form (e.g. an attachments panel). -->
      <slot name="body-after"/>
    </template>
    <template #footer>
      <ModalFooterActions :form="formId"
                          :loading="loading"
                          :submit-color="submitColor"
                          :submit-icon="submitIcon"
                          :submit-label="submitLabel"
                          @cancel="requestClose(false)"/>
    </template>
  </UModal>
</template>
