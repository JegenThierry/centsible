<script lang="ts" setup>
import adze from 'adze'
import {type Category, CategoryType} from "~/models/category/category";
import type {
  ColumnMapping,
  CsvParseResult,
  ImportColumnField,
  ImportPayloadRow,
} from "~/models/transactions/csv-import";
import {type DateFormat, parseAmount, parseCsv, parseToIsoDate} from "~/utils/csv";
import {useTransactionService} from "~/services/transactions/transaction-service";
import {useCategoryService} from "~/services/category/category-service";
import {useBudgetAccountsStore} from "~/stores/budgetAccountsStore";
import {useToasts} from "~/services/toasts/toast-service";
import {useApiErrors} from "~/composables/use-api-errors";
import CancelButton from "~/components/_molecules/buttons/cancel-button.vue";
import CsvMappingStep from "~/components/_organisms/transactions/modals/csv-mapping-step.vue";
import CsvImportPreviewTable from "~/components/_organisms/transactions/modals/csv-import-preview-table.vue";
import AppInput from "~/components/_atoms/ui/app-input.vue";
import AppButton from "~/components/_atoms/ui/app-button.vue";

const isOpen = defineModel<boolean>('open', {required: true});

const emit = defineEmits<{
  (e: 'imported'): void;
}>();

const api = useApi();
const service = useTransactionService(api);
const categoryService = useCategoryService(api);
const accountsStore = useBudgetAccountsStore();
const toasts = useToasts();
const {t} = useI18n();

type Step = 'upload' | 'map' | 'confirm';
const step = ref<Step>('upload');
const loading = ref(false);

const parsed = ref<CsvParseResult | null>(null);
const fieldByColumn = ref<ImportColumnField[]>([]);
const defaultCategory = ref<Category | undefined>(undefined);
const categories = ref<Category[]>([]);
const previewRows = ref<ImportPayloadRow[]>([]);
const invalidRowCount = ref(0);
const dateFormat = ref<DateFormat>('dd/MM/yyyy');

const dateFormatOptions = computed<{ value: DateFormat; label: string }[]>(() => [
  {value: 'dd/MM/yyyy', label: t('transactions.import.dateFormat.dayFirst')},
  {value: 'MM/dd/yyyy', label: t('transactions.import.dateFormat.monthFirst')},
  {value: 'yyyy-MM-dd', label: t('transactions.import.dateFormat.iso')},
  {value: 'auto', label: t('transactions.import.dateFormat.auto')},
]);

const modalTitle = computed(() => {
  if (step.value === 'upload') return t('transactions.import.titleUpload');
  if (step.value === 'map') return t('transactions.import.titleMap');
  return t('transactions.import.titleConfirm');
});

const modalDescription = computed(() => {
  if (step.value === 'upload') return t('transactions.import.descUpload');
  if (step.value === 'map') return t('transactions.import.descMap');
  return t('transactions.import.descConfirm');
});

function resetFormState() {
  step.value = 'upload';
  parsed.value = null;
  fieldByColumn.value = [];
  previewRows.value = [];
  invalidRowCount.value = 0;
  defaultCategory.value = undefined;
  dateFormat.value = 'dd/MM/yyyy';
}

watch(isOpen, async (open) => {
  // Reset on both edges so the modal is always fresh — works whether the parent uses v-if or
  // v-show. The defensive reset on close also clears parsed CSV contents from memory promptly.
  resetFormState();
  if (!open) return;
  if (categories.value.length === 0) {
    try {
      categories.value = await categoryService.fetchCategories();
    } catch (error) {
      adze.ns('imports').error('Failed to load categories', error);
    }
  }
});

async function onFileChange(event: Event) {
  const file = (event.target as HTMLInputElement).files?.[0];
  if (!file) return;
  try {
    const text = await file.text();
    const result = parseCsv(text);
    if (result.headers.length === 0 || result.rows.length === 0) {
      toasts.error(t('transactions.import.fileEmptyTitle'), t('transactions.import.fileEmptyBody'));
      return;
    }
    parsed.value = result;
    fieldByColumn.value = result.headers.map(h => guessField(h));
    step.value = 'map';
  } catch (error) {
    adze.ns('imports').error('CSV parse failed', error);
    toasts.error(t('transactions.import.fileErrorTitle'), t('transactions.import.fileErrorBody'));
  }
}

function guessField(header: string): ImportColumnField {
  const h = header.toLowerCase();
  if (/(date|datum|when)/.test(h)) return 'date';
  if (/(amount|value|montant|betrag|total)/.test(h)) return 'amount';
  if (/(desc|memo|note|reference|libell|payee|narrative)/.test(h)) return 'description';
  if (/(category|categorie|kategorie)/.test(h)) return 'category';
  return 'ignore';
}

const mapping = computed<ColumnMapping>(() => {
  const m: ColumnMapping = {date: null, amount: null, description: null, category: null};
  fieldByColumn.value.forEach((field, idx) => {
    if (field === 'date' && m.date === null) m.date = idx;
    if (field === 'amount' && m.amount === null) m.amount = idx;
    if (field === 'description' && m.description === null) m.description = idx;
    if (field === 'category' && m.category === null) m.category = idx;
  });
  return m;
});

const mappingValid = computed(() =>
  mapping.value.date !== null
  && mapping.value.amount !== null
  && mapping.value.description !== null
);

const expenseCategories = computed(() =>
  categories.value.filter(c => c.type === CategoryType.EXPENSE)
);

function buildPayload() {
  const result: ImportPayloadRow[] = [];
  let invalid = 0;
  const m = mapping.value;
  if (!parsed.value) return {result, invalid};

  for (const row of parsed.value.rows) {
    const rawDate = m.date !== null ? row.cells[m.date] : undefined;
    const rawAmount = m.amount !== null ? row.cells[m.amount] : undefined;
    const rawDesc = m.description !== null ? row.cells[m.description] : undefined;

    const isoDate = rawDate ? parseToIsoDate(rawDate, dateFormat.value) : null;
    const amount = rawAmount ? Math.abs(parseAmount(rawAmount)) : NaN;
    const desc = (rawDesc ?? '').trim();
    const category = defaultCategory.value;

    if (!isoDate || !Number.isFinite(amount) || amount <= 0 || !desc || !category) {
      invalid++;
      continue;
    }

    result.push({
      amount: Number(amount.toFixed(2)),
      categoryId: category.id,
      description: desc.slice(0, 255),
      transactionDate: isoDate,
    });
  }
  return {result, invalid};
}

function goToConfirm() {
  if (!mappingValid.value || !defaultCategory.value) return;
  const {result, invalid} = buildPayload();
  previewRows.value = result;
  invalidRowCount.value = invalid;
  step.value = 'confirm';
}

async function handleImport() {
  if (!accountsStore.activeAccount?.id) return;
  if (previewRows.value.length === 0) return;

  loading.value = true;
  try {
    const result = await service.importBatch(accountsStore.activeAccount.id, previewRows.value);
    toasts.success(
      t('transactions.import.successTitle'),
      t('transactions.import.successBody', {imported: result.imported, skipped: result.skippedDuplicates}),
    );
    emit('imported');
    isOpen.value = false;
  } catch (error) {
    useApiErrors().toastError(error, t('transactions.import.errorTitle'), t('transactions.import.errorBody'));
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <UModal v-model:open="isOpen"
          :description="modalDescription"
          :title="modalTitle"
          :ui="{content: 'max-w-2xl'}">
    <template #body>
      <div v-if="step === 'upload'" class="space-y-4">
        <p class="text-sm text-neutral-500">
          {{ t('transactions.import.uploadHint') }}
        </p>
        <AppInput accept=".csv,text/csv"
                  class="w-full"
                  type="file"
                  @change="onFileChange"/>
      </div>

      <CsvMappingStep v-else-if="step === 'map'"
                      v-model:field-by-column="fieldByColumn"
                      v-model:default-category="defaultCategory"
                      v-model:date-format="dateFormat"
                      :headers="parsed?.headers ?? []"
                      :first-row="parsed?.rows[0]?.cells ?? []"
                      :expense-categories="expenseCategories"
                      :date-format-options="dateFormatOptions"
                      :mapping-valid="mappingValid"/>

      <CsvImportPreviewTable v-else
                             :rows="previewRows"
                             :invalid-count="invalidRowCount"/>
    </template>

    <template #footer>
      <div class="flex justify-between gap-2 w-full">
        <AppButton v-if="step !== 'upload'"
                   color="neutral"
                   variant="ghost"
                   @click="step = step === 'confirm' ? 'map' : 'upload'">
          {{ t('common.actions.back') }}
        </AppButton>
        <div v-else></div>
        <div class="flex gap-2">
          <CancelButton @click="isOpen = false"/>
          <AppButton v-if="step === 'map'"
                     :disabled="!mappingValid || !defaultCategory"
                     @click="goToConfirm">
            {{ t('common.actions.next') }}
          </AppButton>
          <AppButton v-if="step === 'confirm'"
                     :disabled="previewRows.length === 0"
                     :loading="loading"
                     @click="handleImport">
            {{ t('transactions.import.importRows', {count: previewRows.length}) }}
          </AppButton>
        </div>
      </div>
    </template>
  </UModal>
</template>
