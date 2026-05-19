<script lang="ts" setup>
import {computed, h, ref, resolveComponent} from 'vue';
import type {TableColumn} from '@nuxt/ui';
import {useIntersectionObserver} from '@vueuse/core';
import {useAttachmentService} from "~/services/transactions/attachment-service";
import {useApiErrors} from "~/composables/use-api-errors";
import {useToasts} from "~/services/toasts/toast-service";
import type {EnrichedAttachment} from "~/models/transactions/attachment";
import BaseTable from "~/components/_molecules/tables/base-table.vue";
import TableRowActionsMenu from "~/components/_molecules/tables/table-row-actions-menu.vue";
import FormattedDate from "~/components/_atoms/labels/formatted-date.vue";
import LoadingAnimation from "~/components/_atoms/animations/loading-animation.vue";
import {formatBytes} from "~/utils/format";
import {iconFor} from "~/utils/file-type";
import {openBlobInNewTab} from "~/utils/blob-download";

const PAGE_SIZE = 25;

const api = useApi();
const service = useAttachmentService(api);
const {toastError} = useApiErrors();
const toasts = useToasts();
const {t} = useI18n();
const UIcon = resolveComponent('UIcon');
const NuxtLink = resolveComponent('NuxtLink');

const items = ref<EnrichedAttachment[]>([]);
const loading = ref(false);
const hasMore = ref(true);
const page = ref(1);

const showInitialLoading = computed(() => loading.value && items.value.length === 0);
const showLoadMoreSpinner = computed(() => loading.value && items.value.length > 0);

async function load(reset = false) {
  if (reset) {
    items.value = [];
    page.value = 1;
    hasMore.value = true;
  }

  if (loading.value || !hasMore.value) return;
  loading.value = true;

  try {
    const fetched = await service.listForUser(page.value, PAGE_SIZE);
    items.value = reset ? fetched : [...items.value, ...fetched];
    hasMore.value = fetched.length === PAGE_SIZE;
    if (hasMore.value) page.value += 1;
  } catch (error) {
    toastError(error, t('attachments.errors.loadTitle'), t('attachments.errors.loadBody'));
  } finally {
    loading.value = false;
  }
}

async function download(file: EnrichedAttachment) {
  try {
    const blob = await service.fetchBlob(file.transactionId, file.id);
    openBlobInNewTab(blob);
  } catch (error) {
    toastError(error, t('attachments.errors.downloadTitle'), t('attachments.errors.downloadBody'));
  }
}

async function remove(file: EnrichedAttachment) {
  if (!confirm(t('attachments.confirmDelete', {filename: file.filename}))) return;
  try {
    await service.remove(file.transactionId, file.id);
    items.value = items.value.filter(x => x.id !== file.id);
    toasts.success(t('attachments.toasts.deletedTitle'), t('attachments.toasts.deletedBody'));
  } catch (error) {
    toastError(error, t('attachments.errors.deleteTitle'), t('attachments.errors.deleteBody'));
  }
}

const columns = computed<TableColumn<EnrichedAttachment>[]>(() => [
  {
    accessorKey: 'filename',
    header: t('attachments.table.file'),
    cell: ({row}) => h('div', {class: 'flex items-center gap-2 min-w-0'}, [
      h(UIcon, {name: iconFor(row.original.contentType), class: 'text-muted shrink-0'}),
      h('span', {class: 'truncate'}, row.original.filename),
    ]),
  },
  {
    accessorKey: 'sizeBytes',
    header: t('attachments.table.size'),
    meta: {class: {th: 'text-right', td: 'text-right tabular-nums text-muted'}},
    cell: ({row}) => formatBytes(row.original.sizeBytes),
  },
  {
    accessorKey: 'transactionDescription',
    header: t('attachments.table.transaction'),
    cell: ({row}) => h(
      NuxtLink,
      {
        to: `/${row.original.accountId}/transactions`,
        class: 'text-primary hover:underline truncate block max-w-xs',
      },
      () => row.original.transactionDescription?.trim() || t('attachments.table.noDescription'),
    ),
  },
  {
    accessorKey: 'transactionDate',
    header: t('attachments.table.transactionDate'),
    cell: ({row}) => h(FormattedDate, {date: row.original.transactionDate, format: 'full'}),
  },
  {
    accessorKey: 'createdAt',
    header: t('attachments.table.uploadedAt'),
    cell: ({row}) => h(FormattedDate, {date: row.original.createdAt, format: 'full'}),
  },
  {
    id: 'actions',
    meta: {class: {td: 'text-right'}},
    cell: ({row}) => h(TableRowActionsMenu, {
      menuLabel: t('attachments.table.actionsAria'),
      items: [
        {
          label: t('attachments.actions.download'),
          icon: 'i-lucide-download',
          onSelect: () => download(row.original),
        },
        {
          label: t('attachments.actions.openTransaction'),
          icon: 'i-lucide-arrow-right-left',
          to: `/${row.original.accountId}/transactions`,
        },
        {
          label: t('attachments.actions.delete'),
          icon: 'i-lucide-trash',
          color: 'error' as any,
          onSelect: () => remove(row.original),
        },
      ],
    }),
  },
]);

const loadMoreTrigger = ref<HTMLElement | null>(null);

useIntersectionObserver(loadMoreTrigger, async (entries) => {
  const entry = entries[0];
  if (!entry?.isIntersecting) return;
  if (loading.value || !hasMore.value) return;
  await load();
});

onMounted(() => load(true));
</script>

<template>
  <div class="space-y-3">
    <BaseTable :columns="columns"
               :data="items"
               :empty-icon="'i-lucide-paperclip'"
               :empty-title="t('attachments.empty')"
               :loading="showInitialLoading"
               :loading-message="t('attachments.loading')"/>

    <div v-if="hasMore && items.length > 0" ref="loadMoreTrigger" class="flex justify-center p-4">
      <LoadingAnimation v-if="showLoadMoreSpinner"/>
    </div>
  </div>
</template>
