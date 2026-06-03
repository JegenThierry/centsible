<script lang="ts" setup>
import adze from 'adze'
import type {Attachment} from "~/models/transactions/attachment";
import {useAttachmentService} from "~/services/transactions/attachment-service";
import {useApiErrors} from "~/composables/use-api-errors";
import {formatBytes} from "~/utils/format";
import {iconFor} from "~/utils/file-type";
import {openBlobInNewTab} from "~/utils/blob-download";
import AppButton from "~/components/_atoms/ui/app-button.vue";

const props = defineProps<{
  transactionId: string;
  count: number;
}>();

const service = useAttachmentService(useApi());
const {toastError} = useApiErrors();
const {t} = useI18n();

const open = ref(false);
const files = ref<Attachment[] | null>(null);
const loading = ref(false);
const downloadingId = ref<string | null>(null);

async function load() {
  if (files.value !== null || loading.value) return;
  loading.value = true;
  try {
    files.value = await service.list(props.transactionId);
  } catch (error) {
    adze.ns('attachments').error('Failed to load attachments', error);
    files.value = [];
  } finally {
    loading.value = false;
  }
}

watch(open, (isOpen) => {
  if (isOpen) load();
});

async function download(file: Attachment) {
  if (downloadingId.value) return;
  downloadingId.value = file.id;
  try {
    const blob = await service.fetchBlob(props.transactionId, file.id);
    openBlobInNewTab(blob);
  } catch (error) {
    toastError(error, t('transactions.attachments.errors.downloadTitle'), t('transactions.attachments.errors.downloadBody'));
  } finally {
    downloadingId.value = null;
  }
}
</script>

<template>
  <UPopover v-model:open="open" :ui="{content: 'p-0 w-80'}">
    <AppButton :aria-label="t('transactions.attachments.popover.triggerAria', {count})"
             color="neutral"
             icon="i-lucide-paperclip"
             size="xs"
             variant="ghost">
      {{ count }}
    </AppButton>

    <template #content>
      <div class="p-2 space-y-1.5">
        <p class="px-1.5 pt-1 text-xs font-medium text-muted">
          {{ t('transactions.attachments.title') }}
        </p>

        <p v-if="loading" class="px-1.5 py-2 text-xs text-muted italic">
          {{ t('transactions.attachments.popover.loading') }}
        </p>

        <ul v-else-if="files && files.length > 0" class="space-y-1">
          <li v-for="file in files"
              :key="file.id"
              class="flex items-center gap-2 rounded-md hover:bg-elevated px-2 py-1.5">
            <UIcon :name="iconFor(file.contentType)" class="text-muted shrink-0"/>
            <button class="flex-1 truncate text-sm text-left text-default hover:text-primary"
                    :disabled="downloadingId === file.id"
                    type="button"
                    @click="download(file)">
              {{ file.filename }}
            </button>
            <span class="text-xs text-muted tabular-nums shrink-0">{{ formatBytes(file.sizeBytes) }}</span>
          </li>
        </ul>

        <p v-else class="px-1.5 py-2 text-xs text-muted italic">
          {{ t('transactions.attachments.popover.empty') }}
        </p>
      </div>
    </template>
  </UPopover>
</template>
