<script lang="ts" setup>
import adze from 'adze'
import {type Category} from "~/models/category/category";
import {Frequency, type RecurringTransactionForm} from "~/models/recurring/recurring-transaction";
import BaseInput from "~/components/_atoms/inputs/base-input.vue";
import CategorySelect from "~/components/_atoms/inputs/category-select.vue";
import CurrencySelect from "~/components/_atoms/inputs/currency-select.vue";
import DateInput from "~/components/_atoms/inputs/date-input.vue";
import FrequencySelect from "~/components/_atoms/inputs/frequency-select.vue";
import CategoryTypeBadge from "~/components/_molecules/badges/category-type-badge.vue";
import FormattedDate from "~/components/_atoms/labels/formatted-date.vue";
import {useCategoryService} from "~/services/category/category-service";
import {ISO_DATE} from "~/utils/date";
import {AMOUNT_INPUT} from "~/utils/money";
import {addDays, addMonths, addWeeks, addYears, format, parseISO} from 'date-fns';

const props = defineProps<{
  modelValue: RecurringTransactionForm;
  disabled?: boolean;
}>();

const emit = defineEmits(['update:modelValue']);

const api = useApi();
const categoryService = useCategoryService(api);
const categories = ref<Category[]>([]);
const {t} = useI18n();

const amountInput = ref<InstanceType<typeof BaseInput>>();
const descriptionInput = ref<InstanceType<typeof BaseInput>>();
const categoryInput = ref();
const frequencyInput = ref();
const startDateInput = ref();

async function loadCategories() {
  try {
    categories.value = await categoryService.fetchCategories();
  } catch (error) {
    adze.ns('recurring').error('Failed to load categories', error);
  }
}

const form = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val),
});

function advance(date: Date, freq: Frequency): Date {
  switch (freq) {
    case Frequency.DAILY: return addDays(date, 1);
    case Frequency.WEEKLY: return addWeeks(date, 1);
    case Frequency.MONTHLY: return addMonths(date, 1);
    case Frequency.YEARLY: return addYears(date, 1);
  }
}

const upcomingOccurrences = computed<string[]>(() => {
  if (!form.value.startDate) return [];
  const start = parseISO(form.value.startDate);
  if (Number.isNaN(start.getTime())) return [];
  const end = form.value.endDate ? parseISO(form.value.endDate) : null;
  const out: string[] = [];
  let cur = start;
  for (let i = 0; i < 5; i++) {
    if (end && cur > end) break;
    out.push(format(cur, ISO_DATE));
    cur = advance(cur, form.value.frequency);
  }
  return out;
});

onMounted(() => {
  loadCategories();
});

defineExpose({
  validate: () => useValidator().validateInputs([
    amountInput,
    descriptionInput,
    categoryInput,
    frequencyInput,
    startDateInput,
  ]),
});
</script>

<template>
  <div class="space-y-4">
    <CategorySelect ref="categoryInput"
                    v-model="form.category"
                    :disabled="disabled"
                    :options="categories"
                    :label="t('transactions.recurring.form.category')"
                    required/>

    <div v-if="form.category" class="flex items-center gap-2 text-sm">
      <span class="text-neutral-500">{{ t('transactions.recurring.form.transactionType') }}</span>
      <CategoryTypeBadge :type="form.category.type"/>
    </div>

    <UFormField :label="t('transactions.recurring.form.currency')" name="currency">
      <CurrencySelect v-model="form.currency"
                      :disabled="disabled"
                      :placeholder="t('transactions.recurring.form.currencyPlaceholder')"/>
    </UFormField>

    <BaseInput ref="amountInput"
               v-model="form.amount"
               :max="AMOUNT_INPUT.max"
               :min="AMOUNT_INPUT.min"
               :disabled="disabled"
               :label="t('transactions.recurring.form.amount')"
               :placeholder="t('transactions.recurring.form.amountPlaceholder')"
               :trailing-text="form.currency"
               required
               type="number"/>

    <BaseInput ref="descriptionInput"
               v-model="form.description"
               :max-length="255"
               :disabled="disabled"
               :label="t('transactions.recurring.form.description')"
               :placeholder="t('transactions.recurring.form.descriptionPlaceholder')"
               required
               type="text"/>

    <FrequencySelect ref="frequencyInput"
                     v-model="form.frequency"
                     :disabled="disabled"
                     :label="t('transactions.recurring.form.frequency')"
                     required/>

    <DateInput ref="startDateInput"
               v-model="form.startDate"
               :description="t('transactions.recurring.form.startDateHelp')"
               :disabled="disabled"
               :label="t('transactions.recurring.form.startDate')"
               required/>

    <DateInput v-model="form.endDate"
               :description="t('transactions.recurring.form.endDateHelp')"
               :disabled="disabled"
               :label="t('transactions.recurring.form.endDate')"/>

    <div v-if="upcomingOccurrences.length > 0" class="rounded-md ring-1 ring-default bg-elevated/40 p-3">
      <div class="text-xs font-semibold text-muted uppercase tracking-wide mb-2">
        {{ t('transactions.recurring.form.previewTitle') }}
      </div>
      <ol class="space-y-1 text-sm">
        <li v-for="(date, idx) in upcomingOccurrences"
            :key="date"
            class="flex items-center gap-2 text-default">
          <span class="text-muted tabular-nums w-5">{{ idx + 1 }}.</span>
          <FormattedDate :date="date" format="date"/>
        </li>
      </ol>
      <p class="text-xs text-muted mt-2">{{ t('transactions.recurring.form.previewHint') }}</p>
    </div>
  </div>
</template>
