<script lang="ts" setup>
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
import CategorySelect from "~/components/_atoms/inputs/category-select.vue";
import CsvMappingRow from "~/components/_molecules/transactions/csv-mapping-row.vue";

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

watch(isOpen, async (open) => {
  if (!open) return;
  step.value = 'upload';
  parsed.value = null;
  fieldByColumn.value = [];
  previewRows.value = [];
  invalidRowCount.value = 0;
  defaultCategory.value = undefined;
  dateFormat.value = 'dd/MM/yyyy';
  if (categories.value.length === 0) {
    try {
      categories.value = await categoryService.fetchCategories();
    } catch (error) {
      console.error('Failed to load categories', error);
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
    console.error(error);
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
      <!-- Step 1: upload -->
      <div v-if="step === 'upload'" class="space-y-4">
        <p class="text-sm text-neutral-500">
          {{ t('transactions.import.uploadHint') }}
        </p>
        <UInput accept=".csv,text/csv"
                class="w-full"
                type="file"
                @change="onFileChange"/>
      </div>

      <!-- Step 2: map columns -->
      <div v-else-if="step === 'map'" class="space-y-4">
        <div class="border border-neutral-200 dark:border-neutral-800 rounded-md divide-y divide-neutral-200 dark:divide-neutral-800">
          <CsvMappingRow v-for="(header, idx) in parsed?.headers ?? []"
                         :key="idx"
                         v-model="fieldByColumn[idx]!"
                         :header="header"
                         :preview="parsed?.rows[0]?.cells[idx] ?? ''"/>
        </div>

        <CategorySelect v-model="defaultCategory"
                        :options="expenseCategories"
                        :description="t('transactions.import.assignCategoryHelp')"
                        :label="t('transactions.import.assignCategoryLabel')"
                        required/>

        <UFormField :description="t('transactions.import.dateFormatHelp')"
                    :label="t('transactions.import.dateFormatLabel')">
          <USelect v-model="dateFormat"
                   :items="dateFormatOptions"
                   class="w-full"
                   value-key="value"/>
        </UFormField>

        <UAlert v-if="!mappingValid"
                color="warning"
                :description="t('transactions.import.missingMappingsDescription')"
                :title="t('transactions.import.missingMappingsTitle')"
                variant="subtle"/>
      </div>

      <!-- Step 3: confirm -->
      <div v-else class="space-y-4">
        <div class="flex items-center justify-between gap-4 text-sm">
          <p>{{ t('transactions.import.readyToImport', {count: previewRows.length}) }}</p>
          <p v-if="invalidRowCount > 0" class="text-warning">
            {{ t('transactions.import.skippedRows', {count: invalidRowCount}) }}
          </p>
        </div>
        <div class="max-h-64 overflow-y-auto border border-default rounded-md">
          <table class="w-full text-sm">
            <thead class="bg-muted sticky top-0">
              <tr>
                <th class="text-left p-2">{{ t('transactions.table.date') }}</th>
                <th class="text-left p-2">{{ t('transactions.table.description') }}</th>
                <th class="text-right p-2">{{ t('transactions.table.amount') }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(row, i) in previewRows.slice(0, 50)" :key="i" class="border-t border-muted">
                <td class="p-2 tabular-nums">{{ row.transactionDate }}</td>
                <td class="p-2 truncate max-w-[20rem]">{{ row.description }}</td>
                <td class="p-2 text-right tabular-nums">{{ row.amount.toFixed(2) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
        <p v-if="previewRows.length > 50" class="text-xs text-neutral-500">
          {{ t('transactions.import.previewShowingFirst', {count: previewRows.length}) }}
        </p>
      </div>
    </template>

    <template #footer>
      <div class="flex justify-between gap-2 w-full">
        <UButton v-if="step !== 'upload'"
                 color="neutral"
                 variant="ghost"
                 @click="step = step === 'confirm' ? 'map' : 'upload'">
          {{ t('common.actions.back') }}
        </UButton>
        <div v-else></div>
        <div class="flex gap-2">
          <CancelButton @click="isOpen = false"/>
          <UButton v-if="step === 'map'"
                   :disabled="!mappingValid || !defaultCategory"
                   @click="goToConfirm">
            {{ t('common.actions.next') }}
          </UButton>
          <UButton v-if="step === 'confirm'"
                   :disabled="previewRows.length === 0"
                   :loading="loading"
                   @click="handleImport">
            {{ t('transactions.import.importRows', {count: previewRows.length}) }}
          </UButton>
        </div>
      </div>
    </template>
  </UModal>
</template>
