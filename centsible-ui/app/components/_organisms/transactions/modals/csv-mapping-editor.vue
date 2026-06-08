<script lang="ts" setup>
import type {CsvColumnMapping} from "~/models/imports/imports";
import AppSelect from "~/components/_atoms/ui/app-select.vue";
import AppInput from "~/components/_atoms/ui/app-input.vue";
import AppCheckbox from "~/components/_atoms/ui/app-checkbox.vue";

type Role =
  | 'ignore' | 'date' | 'description' | 'amount'
  | 'debit' | 'credit' | 'currency' | 'counterparty' | 'category';

const UNIQUE_ROLES: Role[] = ['date', 'description', 'amount', 'debit', 'credit', 'currency', 'counterparty', 'category'];

const props = defineProps<{
  headers: string[];
  sampleRow: string[];
}>();

const mapping = defineModel<CsvColumnMapping>({required: true});
const {t} = useI18n();

const roleOptions = computed(() =>
  (['date', 'description', 'amount', 'debit', 'credit', 'currency', 'counterparty', 'category', 'ignore'] as Role[])
    .map((value) => ({value, label: t(`transactions.advancedImport.roles.${value}`)})),
);

const decimalOptions = [
  {value: '.', label: '1,234.56'},
  {value: ',', label: '1.234,56'},
];

function roleForColumn(idx: number): Role {
  const m = mapping.value;
  if (m.dateColumn === idx) return 'date';
  if (m.descriptionColumn === idx) return 'description';
  if (m.amountColumn === idx) return 'amount';
  if (m.debitColumn === idx) return 'debit';
  if (m.creditColumn === idx) return 'credit';
  if (m.currencyColumn === idx) return 'currency';
  if (m.counterpartyColumn === idx) return 'counterparty';
  if (m.categoryColumn === idx) return 'category';
  return 'ignore';
}

function setRole(idx: number, role: Role) {
  const roles = props.headers.map((_, i) => roleForColumn(i));
  if (UNIQUE_ROLES.includes(role)) {
    roles.forEach((r, i) => {
      if (r === role && i !== idx) roles[i] = 'ignore';
    });
  }
  roles[idx] = role;
  const find = (r: Role): number | null => {
    const i = roles.indexOf(r);
    return i === -1 ? null : i;
  };
  mapping.value = {
    ...mapping.value,
    dateColumn: find('date') ?? -1,
    descriptionColumn: find('description') ?? -1,
    amountColumn: find('amount'),
    debitColumn: find('debit'),
    creditColumn: find('credit'),
    currencyColumn: find('currency'),
    counterpartyColumn: find('counterparty'),
    categoryColumn: find('category'),
  };
}
</script>

<template>
  <div class="space-y-4">
    <p class="text-sm text-muted">{{ t('transactions.advancedImport.mappingHint') }}</p>

    <div class="border border-default rounded-md divide-y divide-default max-h-60 overflow-auto">
      <div v-for="(header, idx) in headers"
           :key="idx"
           class="grid grid-cols-1 sm:grid-cols-3 items-center gap-3 p-2">
        <div class="text-sm font-medium truncate">{{ header }}</div>
        <div class="text-xs text-muted truncate">
          <span class="text-dimmed">{{ t('transactions.advancedImport.columnPreview') }}</span>
          {{ sampleRow[idx] || '—' }}
        </div>
        <AppSelect :model-value="roleForColumn(idx)"
                   :items="roleOptions"
                   value-key="value"
                   class="w-full"
                   @update:model-value="(v) => setRole(idx, v as Role)"/>
      </div>
    </div>

    <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
      <UFormField :label="t('transactions.advancedImport.dateFormatLabel')"
                  :description="t('transactions.advancedImport.dateFormatHelp')">
        <AppInput v-model="mapping.dateFormat" class="w-full" placeholder="dd/MM/yyyy"/>
      </UFormField>
      <UFormField :label="t('transactions.advancedImport.decimalSeparatorLabel')">
        <AppSelect v-model="mapping.decimalSeparator" :items="decimalOptions" value-key="value" class="w-full"/>
      </UFormField>
    </div>

    <AppCheckbox v-model="mapping.debitsArePositive"
                 :label="t('transactions.advancedImport.debitsArePositiveLabel')"
                 :description="t('transactions.advancedImport.debitsArePositiveHelp')"/>
  </div>
</template>
