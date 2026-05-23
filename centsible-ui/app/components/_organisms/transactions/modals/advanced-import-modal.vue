<script lang="ts" setup>
import {ref, watch} from 'vue';
import adze from 'adze'
import {type Category, CategoryType} from "~/models/category/category";
import {useImportService} from "~/services/imports/import-service";
import {useCategoryService} from "~/services/category/category-service";
import {useBudgetAccountsStore} from "~/stores/budgetAccountsStore";
import {useToasts} from "~/services/toasts/toast-service";
import {useApiErrors} from "~/composables/use-api-errors";
import type {
  CsvProbeResponse,
  ImportDetection,
  ImportPreview,
  ParseHints,
} from "~/models/imports/imports";
import CancelButton from "~/components/_molecules/buttons/cancel-button.vue";
import CategorySelect from "~/components/_atoms/inputs/category-select.vue";

const isOpen = defineModel<boolean>('open', {required: true});

const emit = defineEmits<{
  (e: 'imported'): void;
}>();

type Step = 'upload' | 'review';

const api = useApi();
const importService = useImportService(api);
const categoryService = useCategoryService(api);
const accountsStore = useBudgetAccountsStore();
const toasts = useToasts();
const {t} = useI18n();

const step = ref<Step>('upload');
const file = ref<File | null>(null);
const detection = ref<ImportDetection | null>(null);
const probe = ref<CsvProbeResponse | null>(null);
const preview = ref<ImportPreview | null>(null);
const defaultCategory = ref<Category | undefined>(undefined);
const categories = ref<Category[]>([]);
const loading = ref(false);
const committing = ref(false);

watch(isOpen, async (open) => {
  if (!open) return;
  step.value = 'upload';
  file.value = null;
  detection.value = null;
  probe.value = null;
  preview.value = null;
  defaultCategory.value = undefined;
  if (categories.value.length === 0) {
    try {
      categories.value = await categoryService.fetchCategories();
    } catch (error) {
      adze.ns('imports').error('Failed to load categories', error);
    }
  }
});

const expenseCategories = computed(() =>
  categories.value.filter(c => c.type === CategoryType.EXPENSE)
);

async function onFileChange(event: Event) {
  const f = (event.target as HTMLInputElement).files?.[0];
  if (!f) return;
  file.value = f;
}

function buildHints(): ParseHints {
  return {
    defaultCategoryId: defaultCategory.value?.id,
    csvMapping: probe.value?.suggestedMapping ?? undefined,
    csvDialect: probe.value?.dialect,
  };
}

async function detectAndPreview() {
  if (!file.value || !defaultCategory.value) return;
  loading.value = true;
  try {
    const det = await importService.detect(file.value);
    detection.value = det;
    if (det.parserId === 'csv') {
      probe.value = await importService.csvProbe(file.value);
    } else {
      probe.value = null;
    }
    preview.value = await importService.preview(file.value, det.parserId, buildHints(), 50);
    step.value = 'review';
  } catch (error) {
    useApiErrors().toastError(
      error,
      t('transactions.advancedImport.errorTitle'),
      t('transactions.advancedImport.detectFailedBody'),
    );
  } finally {
    loading.value = false;
  }
}

async function commit() {
  if (!file.value || !detection.value || !accountsStore.activeAccount?.id) return;
  committing.value = true;
  try {
    const result = await importService.commit(
      accountsStore.activeAccount.id,
      file.value,
      detection.value.parserId,
      buildHints(),
    );
    toasts.success(
      t('transactions.advancedImport.successTitle'),
      t('transactions.advancedImport.successBody', {
        imported: result.imported,
        skipped: result.skippedDuplicates,
      }),
    );
    emit('imported');
    isOpen.value = false;
  } catch (error) {
    useApiErrors().toastError(
      error,
      t('transactions.advancedImport.errorTitle'),
      t('transactions.advancedImport.commitFailedBody'),
    );
  } finally {
    committing.value = false;
  }
}
</script>

<template>
  <UModal v-model:open="isOpen"
          :title="step === 'upload' ? t('transactions.advancedImport.titleUpload') : t('transactions.advancedImport.titleReview')"
          :description="step === 'upload' ? t('transactions.advancedImport.descUpload') : t('transactions.advancedImport.descReview')"
          :ui="{content: 'max-w-2xl'}">
    <template #body>
      <div v-if="step === 'upload'" class="space-y-4">
        <p class="text-sm text-neutral-500">
          {{ t('transactions.advancedImport.uploadHint') }}
        </p>
        <UInput accept=".csv,.tsv,.ofx,.qfx,.qbo,text/csv,application/x-ofx"
                class="w-full"
                type="file"
                @change="onFileChange"/>
        <CategorySelect v-model="defaultCategory"
                        :options="expenseCategories"
                        :description="t('transactions.advancedImport.defaultCategoryHelp')"
                        :label="t('transactions.advancedImport.defaultCategoryLabel')"
                        required/>
      </div>

      <div v-else-if="step === 'review'" class="space-y-4">
        <div class="flex items-center gap-2 flex-wrap text-sm">
          <UBadge color="primary" variant="subtle">{{ detection?.displayName ?? '?' }}</UBadge>
          <UBadge v-if="probe?.suggestedProfileId" color="success" variant="subtle">
            {{ probe.suggestedProfileId }} v{{ probe.suggestedProfileVersion }}
          </UBadge>
          <span class="text-neutral-500">
            {{ t('transactions.advancedImport.detectedRows', {count: preview?.totalRows ?? 0}) }}
          </span>
        </div>

        <UAlert v-if="(preview?.warnings?.length ?? 0) > 0"
                color="warning"
                variant="subtle"
                :title="t('transactions.advancedImport.warningsTitle', {count: preview?.warnings.length ?? 0})">
          <template #description>
            <ul class="list-disc pl-5 max-h-32 overflow-auto text-xs">
              <li v-for="(w, i) in preview?.warnings.slice(0, 20)" :key="i">
                <span v-if="w.sourceRow !== null && w.sourceRow !== undefined">#{{ w.sourceRow }}: </span>
                {{ w.message }}
              </li>
            </ul>
          </template>
        </UAlert>

        <div class="max-h-72 overflow-auto border border-default rounded-md">
          <table class="w-full text-sm">
            <thead class="bg-muted sticky top-0">
              <tr>
                <th class="text-left p-2">{{ t('transactions.table.date') }}</th>
                <th class="text-left p-2">{{ t('transactions.table.description') }}</th>
                <th class="text-right p-2">{{ t('transactions.table.amount') }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(row, i) in preview?.sample" :key="i" class="border-t border-muted">
                <td class="p-2 tabular-nums">{{ row.transactionDate }}</td>
                <td class="p-2 truncate max-w-[20rem]">{{ row.description }}</td>
                <td class="p-2 text-right tabular-nums">{{ Number(row.amount).toFixed(2) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </template>

    <template #footer>
      <div class="flex justify-between gap-2 w-full">
        <UButton v-if="step === 'review'"
                 color="neutral"
                 variant="ghost"
                 @click="step = 'upload'">
          {{ t('common.actions.back') }}
        </UButton>
        <div v-else></div>
        <div class="flex gap-2">
          <CancelButton @click="isOpen = false"/>
          <UButton v-if="step === 'upload'"
                   :disabled="!file || !defaultCategory"
                   :loading="loading"
                   @click="detectAndPreview">
            {{ t('common.actions.next') }}
          </UButton>
          <UButton v-if="step === 'review'"
                   :disabled="!preview || preview.totalRows === 0"
                   :loading="committing"
                   @click="commit">
            {{ t('transactions.advancedImport.importRows', {count: preview?.totalRows ?? 0}) }}
          </UButton>
        </div>
      </div>
    </template>
  </UModal>
</template>
