<script lang="ts" setup>
import adze from 'adze'
import type {Attachment} from "~/models/transactions/attachment";
import {useAttachmentService} from "~/services/transactions/attachment-service";
import {useToasts} from "~/services/toasts/toast-service";
import {formatBytes} from "~/utils/format";
import {iconFor} from "~/utils/file-type";
import {openBlobInNewTab} from "~/utils/blob-download";

const props = defineProps<{
  transactionId?: string;
}>();

const service = useAttachmentService(useApi());
const toasts = useToasts();
const {t} = useI18n();

type PendingFile = {id: string; file: File};

const attachments = ref<Attachment[]>([]);
const pending = ref<PendingFile[]>([]);

function makePendingId(): string {
  return typeof crypto !== 'undefined' && 'randomUUID' in crypto
    ? crypto.randomUUID()
    : `pending-${Date.now()}-${Math.random().toString(36).slice(2)}`;
}
const loading = ref(false);
const uploading = ref(false);
const fileInput = ref<HTMLInputElement | null>(null);

const ACCEPTED = '.pdf,.png,.jpg,.jpeg,.webp';
const MAX_BYTES = 10 * 1024 * 1024;
const ALLOWED_TYPES = new Set([
  'application/pdf',
  'image/jpeg',
  'image/png',
  'image/webp',
]);

const pendingCount = computed(() => pending.value.length);
const isEmpty = computed(() => attachments.value.length === 0 && pending.value.length === 0);

async function refresh() {
  if (!props.transactionId) return;
  loading.value = true;
  try {
    attachments.value = await service.list(props.transactionId);
  } catch (error) {
    adze.ns('attachments').error('Failed to load attachments', error);
    attachments.value = [];
  } finally {
    loading.value = false;
  }
}

function validate(file: File): boolean {
  if (!ALLOWED_TYPES.has(file.type)) {
    toasts.error(t('transactions.attachments.errors.typeTitle'), t('transactions.attachments.errors.typeBody'));
    return false;
  }
  if (file.size > MAX_BYTES) {
    toasts.error(t('transactions.attachments.errors.sizeTitle'), t('transactions.attachments.errors.sizeBody'));
    return false;
  }
  return true;
}

function onPick() {
  fileInput.value?.click();
}

async function onFileChange(event: Event) {
  const target = event.target as HTMLInputElement;
  const file = target.files?.[0];
  target.value = '';
  if (!file) return;
  if (!validate(file)) return;

  if (!props.transactionId) {
    pending.value = [...pending.value, {id: makePendingId(), file}];
    return;
  }

  uploading.value = true;
  try {
    const saved = await service.upload(props.transactionId, file);
    attachments.value = [...attachments.value, saved];
    toasts.success(t('transactions.attachments.toasts.uploadedTitle'), t('transactions.attachments.toasts.uploadedBody'));
  } catch (error) {
    adze.ns('attachments').error('Upload failed', error);
    toasts.error(t('transactions.attachments.errors.uploadTitle'), t('transactions.attachments.errors.uploadBody'));
  } finally {
    uploading.value = false;
  }
}

async function download(a: Attachment) {
  if (!props.transactionId) return;
  try {
    const blob = await service.fetchBlob(props.transactionId, a.id);
    openBlobInNewTab(blob);
  } catch (error) {
    adze.ns('attachments').error('Download failed', error);
    toasts.error(t('transactions.attachments.errors.downloadTitle'), t('transactions.attachments.errors.downloadBody'));
  }
}

async function remove(a: Attachment) {
  if (!props.transactionId) return;
  try {
    await service.remove(props.transactionId, a.id);
    attachments.value = attachments.value.filter(x => x.id !== a.id);
    toasts.success(t('transactions.attachments.toasts.deletedTitle'), t('transactions.attachments.toasts.deletedBody'));
  } catch (error) {
    adze.ns('attachments').error('Delete failed', error);
    toasts.error(t('transactions.attachments.errors.deleteTitle'), t('transactions.attachments.errors.deleteBody'));
  }
}

function removePending(id: string) {
  pending.value = pending.value.filter(p => p.id !== id);
}

async function uploadPending(transactionId: string): Promise<{uploaded: number; failed: File[]}> {
  if (pending.value.length === 0) return {uploaded: 0, failed: []};
  const failed: PendingFile[] = [];
  let uploaded = 0;
  uploading.value = true;
  try {
    for (const item of pending.value) {
      try {
        await service.upload(transactionId, item.file);
        uploaded++;
      } catch (error) {
        adze.ns('attachments').error('Pending upload failed', item.file.name, error);
        failed.push(item);
      }
    }
  } finally {
    uploading.value = false;
    pending.value = failed;
  }
  return {uploaded, failed: failed.map(f => f.file)};
}

function clearPending() {
  pending.value = [];
}

defineExpose({uploadPending, clearPending, pendingCount});

watch(() => props.transactionId, () => refresh(), {immediate: true});
</script>

<template>
  <div class="space-y-3">
    <div class="flex items-center justify-between">
      <h4 class="text-sm font-semibold">{{ t('transactions.attachments.title') }}</h4>
      <UButton :loading="uploading"
               icon="i-lucide-paperclip"
               size="xs"
               variant="soft"
               @click="onPick">
        {{ t('transactions.attachments.add') }}
      </UButton>
      <input ref="fileInput"
             :accept="ACCEPTED"
             class="hidden"
             type="file"
             @change="onFileChange"/>
    </div>

    <p class="text-xs text-muted">{{ t('transactions.attachments.hint') }}</p>

    <ul v-if="!isEmpty" class="space-y-1.5">
      <li v-for="a in attachments"
          :key="a.id"
          class="flex items-center gap-2 text-sm rounded-md ring-1 ring-default px-2.5 py-1.5">
        <UIcon :name="iconFor(a.contentType)" class="text-muted shrink-0"/>
        <button class="flex-1 truncate text-default hover:text-primary text-left"
                type="button"
                @click="download(a)">
          {{ a.filename }}
        </button>
        <span class="text-xs text-muted tabular-nums shrink-0">{{ formatBytes(a.sizeBytes) }}</span>
        <UButton :aria-label="t('transactions.attachments.deleteAria')"
                 color="error"
                 icon="i-lucide-trash"
                 size="xs"
                 variant="ghost"
                 @click="remove(a)"/>
      </li>
      <li v-for="item in pending"
          :key="item.id"
          class="flex items-center gap-2 text-sm rounded-md ring-1 ring-dashed ring-default px-2.5 py-1.5">
        <UIcon name="i-lucide-clock" class="text-muted shrink-0"/>
        <span class="flex-1 truncate text-default text-left">{{ item.file.name }}</span>
        <span class="text-xs text-muted italic shrink-0">{{ t('transactions.attachments.pending') }}</span>
        <span class="text-xs text-muted tabular-nums shrink-0">{{ formatBytes(item.file.size) }}</span>
        <UButton :aria-label="t('transactions.attachments.deleteAria')"
                 color="error"
                 icon="i-lucide-trash"
                 size="xs"
                 variant="ghost"
                 @click="removePending(item.id)"/>
      </li>
    </ul>

    <p v-else-if="!loading" class="text-xs text-muted italic">
      {{ t('transactions.attachments.empty') }}
    </p>
  </div>
</template>
