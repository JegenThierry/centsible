<script lang="ts" setup>
import adze from 'adze'
import {type Category, CategoryType} from "~/models/category/category";
import type {BudgetAccount} from "~/models/budget-account/budget-account";
import type {Currency} from "~/models/budget-account/currency";
import {Frequency, type RecurringTransactionForm} from "~/models/recurring/recurring-transaction";
import AccountSelect from "~/components/_atoms/inputs/account-select.vue";
import AppRadioGroup from "~/components/_atoms/ui/app-radio-group.vue";
import BaseInput from "~/components/_atoms/inputs/base-input.vue";
import CategorySelect from "~/components/_atoms/inputs/category-select.vue";
import CurrencySelect from "~/components/_atoms/inputs/currency-select.vue";
import DateInput from "~/components/_atoms/inputs/date-input.vue";
import FrequencySelect from "~/components/_atoms/inputs/frequency-select.vue";
import FormattedDate from "~/components/_atoms/labels/formatted-date.vue";
import {useCategoryService} from "~/services/category/category-service";
import {ISO_DATE} from "~/utils/date";
import {AMOUNT_INPUT} from "~/utils/money";
import {addDays, addMonths, addWeeks, addYears, format, parseISO} from 'date-fns';

const props = defineProps<{
  modelValue: RecurringTransactionForm;
  disabled?: boolean;
  sourceLocked?: boolean;
}>();

const emit = defineEmits(['update:modelValue']);

const api = useApi();
const categoryService = useCategoryService(api);
const categories = ref<Category[]>([]);
const budgetAccountsStore = useBudgetAccountsStore();
const {t} = useI18n();

const userTouchedType = ref(false);

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

const accounts = computed<BudgetAccount[]>(() => budgetAccountsStore.availableAccounts);

const mode = computed<'standard' | 'transfer'>({
  get: () => form.value.isTransfer ? 'transfer' : 'standard',
  set: (value) => { form.value.isTransfer = value === 'transfer'; },
});

const modeOptions = computed(() => [
  {label: t('transactions.recurring.form.modeStandard'), value: 'standard'},
  {label: t('transactions.recurring.form.modeTransfer'), value: 'transfer'},
]);

const typeOptions = computed(() => [
  {label: t('transactions.form.typeIncome'), value: CategoryType.INCOME},
  {label: t('transactions.form.typeExpense'), value: CategoryType.EXPENSE},
]);

function onCategoryPicked(cat: Category | undefined) {
  if (cat && !userTouchedType.value) form.value.type = cat.type;
}

function onTypeChange(value: CategoryType) {
  form.value.type = value;
  userTouchedType.value = true;
}

const sourceAccount = computed<BudgetAccount | undefined>({
  get: () => accounts.value.find(a => a.id === form.value.sourceAccountId),
  set: (account) => { form.value.sourceAccountId = account?.id; },
});
const destinationAccount = computed<BudgetAccount | undefined>({
  get: () => accounts.value.find(a => a.id === form.value.destinationAccountId),
  set: (account) => { form.value.destinationAccountId = account?.id; },
});

const sourceCurrency = computed<Currency | undefined>(() => sourceAccount.value?.currency);
const amountTrailing = computed(() => form.value.isTransfer ? (sourceCurrency.value ?? '') : form.value.currency);

function advance(date: Date, freq: Frequency): Date {
  switch (freq) {
    case Frequency.DAILY: return addDays(date, 1);
    case Frequency.WEEKLY: return addWeeks(date, 1);
    case Frequency.BIWEEKLY: return addWeeks(date, 2);
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
  if (budgetAccountsStore.availableAccounts.length === 0) budgetAccountsStore.updateAvailableAccounts();
});
</script>

<template>
  <div class="space-y-4">
    <AppRadioGroup v-model="mode"
                   :disabled="disabled"
                   :items="modeOptions"
                   :legend="t('transactions.recurring.form.modeLegend')"
                   orientation="horizontal"/>

    <template v-if="!form.isTransfer">
      <CategorySelect name="category"
                      v-model="form.category"
                      :disabled="disabled"
                      :options="categories"
                      :label="t('transactions.recurring.form.category')"
                      required
                      @update:model-value="onCategoryPicked"/>

      <div class="flex flex-col gap-1">
        <span class="text-sm text-neutral-500">{{ t('transactions.recurring.form.transactionType') }}</span>
        <AppRadioGroup :model-value="form.type"
                       :disabled="disabled"
                       :items="typeOptions"
                       orientation="horizontal"
                       @update:model-value="onTypeChange"/>
      </div>

      <UFormField :label="t('transactions.recurring.form.currency')" name="currency">
        <CurrencySelect v-model="form.currency"
                        :disabled="disabled"
                        :placeholder="t('transactions.recurring.form.currencyPlaceholder')"/>
      </UFormField>
    </template>

    <template v-else>
      <AccountSelect name="sourceAccountId"
                     v-model="sourceAccount"
                     :options="accounts"
                     :label="t('transactions.transfer.fromAccount')"
                     :description="t('transactions.transfer.fromAccountHelp')"
                     :disabled="disabled || sourceLocked"
                     required/>

      <AccountSelect name="destinationAccountId"
                     v-model="destinationAccount"
                     :options="accounts"
                     :label="t('transactions.transfer.toAccount')"
                     :description="t('transactions.transfer.toAccountHelp')"
                     :disabled="disabled"
                     required/>
    </template>

    <BaseInput name="amount"
               v-model="form.amount"
               :max="AMOUNT_INPUT.max"
               :min="AMOUNT_INPUT.min"
               :disabled="disabled"
               :label="t('transactions.recurring.form.amount')"
               :placeholder="t('transactions.recurring.form.amountPlaceholder')"
               :trailing-text="amountTrailing"
               required
               type="number"/>

    <BaseInput name="description"
               v-model="form.description"
               :max-length="255"
               :disabled="disabled"
               :label="t('transactions.recurring.form.description')"
               :placeholder="t('transactions.recurring.form.descriptionPlaceholder')"
               required
               type="text"/>

    <FrequencySelect name="frequency"
                     v-model="form.frequency"
                     :disabled="disabled"
                     :label="t('transactions.recurring.form.frequency')"
                     required/>

    <DateInput name="startDate"
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
