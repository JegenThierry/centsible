<script lang="ts" setup>
import {type Category} from "~/models/category/category";
import {type ImportColumnField} from "~/models/transactions/csv-import";
import {type DateFormat} from "~/utils/csv";
import CategorySelect from "~/components/_atoms/inputs/category-select.vue";
import CsvMappingRow from "~/components/_molecules/transactions/csv-mapping-row.vue";
import AppSelect from "~/components/_atoms/ui/app-select.vue";

defineProps<{
  headers: string[];
  firstRow: string[];
  expenseCategories: Category[];
  dateFormatOptions: { value: DateFormat; label: string }[];
  mappingValid: boolean;
}>();

const fieldByColumn = defineModel<ImportColumnField[]>('fieldByColumn', {required: true});
const defaultCategory = defineModel<Category | undefined>('defaultCategory');
const dateFormat = defineModel<DateFormat>('dateFormat', {required: true});

const {t} = useI18n();
</script>

<template>
  <div class="space-y-4">
    <div class="border border-neutral-200 dark:border-neutral-800 rounded-md divide-y divide-neutral-200 dark:divide-neutral-800">
      <CsvMappingRow v-for="(header, idx) in headers"
                     :key="idx"
                     v-model="fieldByColumn[idx]!"
                     :header="header"
                     :preview="firstRow[idx] ?? ''"/>
    </div>

    <CategorySelect v-model="defaultCategory"
                    :options="expenseCategories"
                    :description="t('transactions.import.assignCategoryHelp')"
                    :label="t('transactions.import.assignCategoryLabel')"
                    required/>

    <UFormField :description="t('transactions.import.dateFormatHelp')"
                :label="t('transactions.import.dateFormatLabel')">
      <AppSelect v-model="dateFormat"
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
</template>
