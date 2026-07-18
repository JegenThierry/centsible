<script lang="ts" setup>
import {ref, watch} from 'vue';
import adze from 'adze'
import {type Category, CategoryType} from "~/models/category/category";
import {useImportService} from "~/services/imports/import-service";
import {useCategoryService} from "~/services/category/category-service";
import {useBudgetAccountsStore} from "~/stores/budgetAccountsStore";
import {useImportTemplatesStore} from "~/stores/importTemplatesStore";
import {useToasts} from "~/services/toasts/toast-service";
import {useApiErrors} from "~/composables/use-api-errors";
import type {
  CsvColumnMapping,
  CsvProbeResponse,
  ImportDetection,
  ImportPreview,
  ParseHints,
} from "~/models/imports/imports";
import CancelButton from "~/components/_molecules/buttons/cancel-button.vue";
import CategorySelect from "~/components/_atoms/inputs/category-select.vue";
import CsvMappingEditor from "~/components/_organisms/transactions/modals/csv-mapping-editor.vue";
import AppButton from "~/components/_atoms/ui/app-button.vue";
import AppInput from "~/components/_atoms/ui/app-input.vue";
import AppSelect from "~/components/_atoms/ui/app-select.vue";
import TransactionAmount from "~/components/_molecules/transactions/transaction-amount.vue";
import {Currency} from "~/models/budget-account/currency";

const isOpen = defineModel<boolean>('open', {required: true});

const emit = defineEmits<{
  (e: 'imported'): void;
}>();

type Step = 'upload' | 'map' | 'review';

const api = useApi();
const importService = useImportService(api);
const categoryService = useCategoryService(api);
const accountsStore = useBudgetAccountsStore();
const toasts = useToasts();
const {toastError} = useApiErrors();
const {t} = useI18n();

const previewCurrency = computed(() => accountsStore.activeAccount?.currency ?? Currency.EUR);

const step = ref<Step>('upload');
const file = ref<File | null>(null);
const detection = ref<ImportDetection | null>(null);
const probe = ref<CsvProbeResponse | null>(null);
const mapping = ref<CsvColumnMapping>(blankMapping());
const preview = ref<ImportPreview | null>(null);
const defaultCategory = ref<Category | undefined>(undefined);
const categories = ref<Category[]>([]);
const loading = ref(false);
const committing = ref(false);

const templatesStore = useImportTemplatesStore();
const selectedProfileId = ref<string | null>(null);
const showSaveProfile = ref(false);
const newProfileName = ref('');
const savingProfile = ref(false);

const profileItems = computed(() =>
  templatesStore.templates.map(tpl => ({label: tpl.name, value: tpl.id})),
);

function blankMapping(): CsvColumnMapping {
  return {
    dateColumn: -1, descriptionColumn: -1, amountColumn: null,
    debitColumn: null, creditColumn: null, currencyColumn: null,
    counterpartyColumn: null, categoryColumn: null,
    dateFormat: 'dd/MM/yyyy', decimalSeparator: '.', thousandsSeparator: null,
    debitsArePositive: false,
  };
}

function guessMapping(header: string[]): CsvColumnMapping {
  const lower = header.map(h => h.toLowerCase());
  const findIdx = (re: RegExp) => lower.findIndex(h => re.test(h));
  const amountIdx = findIdx(/(amount|value|montant|betrag|total)/);
  return {
    ...blankMapping(),
    dateColumn: findIdx(/(date|datum|when)/),
    descriptionColumn: findIdx(/(desc|memo|note|reference|libell|payee|narrative)/),
    amountColumn: amountIdx === -1 ? null : amountIdx,
  };
}

const mappingValid = computed(() => {
  const m = mapping.value;
  const hasAmount = m.amountColumn != null || m.debitColumn != null || m.creditColumn != null;
  return m.dateColumn >= 0 && m.descriptionColumn >= 0 && hasAmount;
});

watch(isOpen, async (open) => {
  if (!open) return;
  step.value = 'upload';
  file.value = null;
  detection.value = null;
  probe.value = null;
  mapping.value = blankMapping();
  preview.value = null;
  defaultCategory.value = undefined;
  selectedProfileId.value = null;
  showSaveProfile.value = false;
  newProfileName.value = '';
  void templatesStore.fetchAll();
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
    csvMapping: detection.value?.parserId === 'csv' ? mapping.value : undefined,
    csvDialect: probe.value?.dialect,
  };
}

async function detectAndProceed() {
  if (!file.value || !defaultCategory.value) return;
  loading.value = true;
  try {
    const det = await importService.detect(file.value);
    detection.value = det;
    if (det.parserId === 'csv') {
      const probed = await importService.csvProbe(file.value);
      probe.value = probed;
      mapping.value = probed.suggestedMapping ?? guessMapping(probed.header);
      const savedMatch = probed.suggestedProfileId
        ? templatesStore.templates.find(tpl => tpl.sourceProfileId === probed.suggestedProfileId)
        : undefined;
      if (savedMatch) {
        mapping.value = {...savedMatch.mapping};
        selectedProfileId.value = savedMatch.id;
      }
      step.value = 'map';
    } else {
      probe.value = null;
      preview.value = await importService.preview(file.value, det.parserId, buildHints(), 50);
      step.value = 'review';
    }
  } catch (error) {
    toastError(
      error,
      t('transactions.advancedImport.errorTitle'),
      t('transactions.advancedImport.detectFailedBody'),
    );
  } finally {
    loading.value = false;
  }
}

async function previewWithMapping() {
  if (!file.value || !detection.value || !mappingValid.value) return;
  loading.value = true;
  try {
    preview.value = await importService.preview(file.value, detection.value.parserId, buildHints(), 50);
    step.value = 'review';
  } catch (error) {
    toastError(
      error,
      t('transactions.advancedImport.errorTitle'),
      t('transactions.advancedImport.detectFailedBody'),
    );
  } finally {
    loading.value = false;
  }
}

function backFromReview() {
  step.value = detection.value?.parserId === 'csv' ? 'map' : 'upload';
}

function applyProfile(id: string | null) {
  selectedProfileId.value = id;
  if (!id) return;
  const tpl = templatesStore.templates.find(p => p.id === id);
  if (tpl) mapping.value = {...tpl.mapping};
}

async function saveProfile() {
  const name = newProfileName.value.trim();
  if (!name || !mappingValid.value) return;
  savingProfile.value = true;
  try {
    const created = await templatesStore.create({
      name,
      sourceProfileId: probe.value?.suggestedProfileId ?? null,
      sourceProfileVersion: probe.value?.suggestedProfileVersion ?? null,
      mapping: mapping.value,
      dialect: probe.value?.dialect ?? {delimiter: ',', quote: '"', hasHeader: true, encoding: 'UTF-8'},
    });
    selectedProfileId.value = created.id;
    showSaveProfile.value = false;
    newProfileName.value = '';
    toasts.success(
      t('transactions.advancedImport.savedProfiles.savedTitle'),
      t('transactions.advancedImport.savedProfiles.savedBody', {name: created.name}),
    );
  } catch (error) {
    toastError(
      error,
      t('transactions.advancedImport.savedProfiles.saveFailedTitle'),
      t('transactions.advancedImport.savedProfiles.saveFailedBody'),
    );
  } finally {
    savingProfile.value = false;
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
    toastError(
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
          :title="t(step === 'upload' ? 'transactions.advancedImport.titleUpload' : step === 'map' ? 'transactions.advancedImport.titleMap' : 'transactions.advancedImport.titleReview')"
          :description="t(step === 'upload' ? 'transactions.advancedImport.descUpload' : step === 'map' ? 'transactions.advancedImport.descMap' : 'transactions.advancedImport.descReview')"
          :ui="{content: 'max-w-2xl'}">
    <template #body>
      <div v-if="step === 'upload'" class="space-y-4">
        <p class="text-sm text-muted">
          {{ t('transactions.advancedImport.uploadHint') }}
        </p>
        <AppInput accept=".csv,.tsv,.ofx,.qfx,.qbo,text/csv,application/x-ofx"
                class="w-full"
                type="file"
                @change="onFileChange"/>
        <CategorySelect v-model="defaultCategory"
                        :options="expenseCategories"
                        :description="t('transactions.advancedImport.defaultCategoryHelp')"
                        :label="t('transactions.advancedImport.defaultCategoryLabel')"
                        required/>
      </div>

      <div v-else-if="step === 'map'" class="space-y-4">
        <div class="flex items-center gap-2 flex-wrap text-sm">
          <UBadge color="primary" variant="subtle">{{ detection?.displayName ?? 'CSV' }}</UBadge>
          <UBadge v-if="probe?.suggestedProfileId" color="success" variant="subtle">
            {{ probe.suggestedProfileId }} v{{ probe.suggestedProfileVersion }}
          </UBadge>
        </div>

        <div class="rounded-md border border-default p-3 space-y-3">
          <div class="flex items-end gap-2 flex-wrap">
            <UFormField :label="t('transactions.advancedImport.savedProfiles.selectLabel')" class="flex-1 min-w-[12rem]">
              <AppSelect :model-value="selectedProfileId"
                         :items="profileItems"
                         value-key="value"
                         :placeholder="t('transactions.advancedImport.savedProfiles.selectPlaceholder')"
                         :disabled="profileItems.length === 0"
                         class="w-full"
                         @update:model-value="(v) => applyProfile(v as string)"/>
            </UFormField>
            <AppButton v-if="!showSaveProfile"
                       color="neutral"
                       variant="subtle"
                       icon="i-lucide-bookmark"
                       :disabled="!mappingValid"
                       @click="showSaveProfile = true">
              {{ t('transactions.advancedImport.savedProfiles.saveAction') }}
            </AppButton>
          </div>
          <div v-if="showSaveProfile" class="flex items-end gap-2 flex-wrap">
            <UFormField :label="t('transactions.advancedImport.savedProfiles.nameLabel')" class="flex-1 min-w-[12rem]">
              <AppInput v-model="newProfileName"
                        :placeholder="t('transactions.advancedImport.savedProfiles.namePlaceholder')"
                        class="w-full"/>
            </UFormField>
            <AppButton :disabled="!newProfileName.trim() || !mappingValid"
                       :loading="savingProfile"
                       @click="saveProfile">
              {{ t('common.actions.save') }}
            </AppButton>
            <AppButton color="neutral" variant="ghost" @click="showSaveProfile = false">
              {{ t('common.actions.cancel') }}
            </AppButton>
          </div>
          <p class="text-xs text-muted">{{ t('transactions.advancedImport.savedProfiles.hint') }}</p>
        </div>

        <CsvMappingEditor v-model="mapping"
                          :headers="probe?.header ?? []"
                          :sample-row="probe?.sample?.[0] ?? []"/>
        <UAlert v-if="!mappingValid"
                color="warning"
                variant="subtle"
                :title="t('transactions.advancedImport.mappingInvalidTitle')"
                :description="t('transactions.advancedImport.mappingInvalidBody')"/>
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
                <td class="p-2 text-right">
                  <TransactionAmount :amount="Number(row.amount)"
                                     :type="row.type ?? undefined"
                                     :currency="row.currency ?? previewCurrency"/>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </template>

    <template #footer>
      <div class="flex justify-between gap-2 w-full">
        <AppButton v-if="step === 'map'"
                 color="neutral"
                 variant="ghost"
                 @click="step = 'upload'">
          {{ t('common.actions.back') }}
        </AppButton>
        <AppButton v-else-if="step === 'review'"
                 color="neutral"
                 variant="ghost"
                 @click="backFromReview">
          {{ t('common.actions.back') }}
        </AppButton>
        <div v-else></div>
        <div class="flex gap-2">
          <CancelButton @click="isOpen = false"/>
          <AppButton v-if="step === 'upload'"
                   :disabled="!file || !defaultCategory"
                   :loading="loading"
                   @click="detectAndProceed">
            {{ t('common.actions.next') }}
          </AppButton>
          <AppButton v-if="step === 'map'"
                   :disabled="!mappingValid"
                   :loading="loading"
                   @click="previewWithMapping">
            {{ t('common.actions.next') }}
          </AppButton>
          <AppButton v-if="step === 'review'"
                   :disabled="!preview || preview.totalRows === 0"
                   :loading="committing"
                   @click="commit">
            {{ t('transactions.advancedImport.importRows', {count: preview?.totalRows ?? 0}) }}
          </AppButton>
        </div>
      </div>
    </template>
  </UModal>
</template>
