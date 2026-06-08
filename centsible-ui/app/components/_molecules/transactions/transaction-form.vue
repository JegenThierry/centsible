<script lang="ts" setup>
import {type Category, CategoryType} from "~/models/category/category";
import {type TransactionForm, type TransactionSplitRow} from "~/models/transactions/transaction";
import type {Currency} from "~/models/budget-account/currency";
import BaseInput from "~/components/_atoms/inputs/base-input.vue";
import CategorySelect from "~/components/_atoms/inputs/category-select.vue";
import RuleSuggestionHint from "~/components/_atoms/labels/rule-suggestion-hint.vue";
import CurrencySelect from "~/components/_atoms/inputs/currency-select.vue";
import DateInput from "~/components/_atoms/inputs/date-input.vue";
import AppRadioGroup from "~/components/_atoms/ui/app-radio-group.vue";
import BalanceNumberFormat from "~/components/_atoms/labels/balance-number-format.vue";
import {useCategoriesStore} from "~/stores/categoriesStore";
import {useTagsStore} from "~/stores/tagsStore";
import {useConversionPreview} from "~/composables/use-conversion-preview";
import {AMOUNT_INPUT} from "~/utils/money";
import ConversionPreviewHint from "~/components/_molecules/transactions/conversion-preview-hint.vue";

const props = defineProps<{
  filterType?: CategoryType;
  disabled?: boolean;
  accountId?: string;
  accountCurrency?: Currency;
  ruleHint?: string | null;
}>();

const form = defineModel<TransactionForm>({required: true});

const emit = defineEmits<{
  (e: 'manual-category'): void;
}>();

const {t} = useI18n();

const {converted: previewAmount, failed: previewFailed, isForeign: previewIsForeign} = useConversionPreview({
  accountId: computed(() => props.accountId),
  accountCurrency: computed(() => props.accountCurrency),
  amount: computed(() => form.value.amount),
  currency: computed(() => form.value.currency),
  date: computed(() => form.value.transactionDate),
});
const categoriesStore = useCategoriesStore();
const tagsStore = useTagsStore();

const userTouchedType = ref(false);

const visibleCategories = computed(() => {
  const all = categoriesStore.categories;
  return props.filterType ? all.filter(c => c.type === props.filterType) : all;
});

watch(
  () => props.filterType,
  (filter) => {
    userTouchedType.value = false;
    if (filter) form.value.type = filter;
  },
  {immediate: true},
);

function onCategoryPicked(cat: Category | undefined) {
  if (cat && !userTouchedType.value) form.value.type = cat.type;
  emit('manual-category');
}

const typeOptions = computed(() => [
  {label: t('transactions.form.typeIncome'), value: CategoryType.INCOME},
  {label: t('transactions.form.typeExpense'), value: CategoryType.EXPENSE},
]);

function onTypeChange(value: CategoryType) {
  form.value.type = value;
  userTouchedType.value = true;
}

const canSplit = computed(() =>
  !props.accountCurrency || form.value.currency === props.accountCurrency,
);
const isSplit = computed(() => Array.isArray(form.value.splits));

const splitCategoryOptions = computed(() =>
  categoriesStore.categories.filter((c) => c.type === form.value.type),
);

const splitSum = computed(() =>
  (form.value.splits ?? []).reduce((acc, row) => acc + (Number(row.amount) || 0), 0),
);
const splitRemaining = computed(() => Math.round((form.value.amount - splitSum.value) * 100) / 100);
const splitsBalanced = computed(() => Math.abs(splitRemaining.value) < 0.005);

function enableSplit() {
  form.value.splits = [
    {category: form.value.category, amount: form.value.amount},
    {category: undefined, amount: 0},
  ];
}

function disableSplit() {
  form.value.splits = undefined;
}

function onToggleSplit(value: boolean) {
  if (value) enableSplit();
  else disableSplit();
}

function addSplitRow() {
  (form.value.splits ??= []).push({category: undefined, amount: 0});
}

function removeSplitRow(index: number) {
  const rows = form.value.splits;
  if (!rows) return;
  rows.splice(index, 1);
  if (rows.length === 0) form.value.splits = undefined;
}

function onSplitCategoryPicked(row: TransactionSplitRow, cat: Category | undefined) {
  row.category = cat;
}

watch(canSplit, (allowed) => {
  if (!allowed && isSplit.value) disableSplit();
});

watch(() => form.value.splits?.[0]?.category, (cat) => {
  if (isSplit.value) form.value.category = cat;
});

function toggleTag(id: number) {
  const current = form.value.tagIds ?? [];
  form.value.tagIds = current.includes(id) ? current.filter((x) => x !== id) : [...current, id];
}

function isTagSelected(id: number): boolean {
  return (form.value.tagIds ?? []).includes(id);
}

onMounted(() => {
  if (categoriesStore.categories.length === 0) categoriesStore.updateCategories();
  if (tagsStore.tags.length === 0) tagsStore.fetchAll();
});
</script>

<template>
  <div class="space-y-4">
    <div v-if="!isSplit" class="space-y-1">
      <CategorySelect name="category"
                      v-model="form.category"
                      :disabled="disabled"
                      :options="visibleCategories"
                      :label="t('transactions.form.category')"
                      required
                      @update:model-value="onCategoryPicked"/>
      <RuleSuggestionHint :rule="ruleHint"/>
    </div>

    <div v-if="!filterType" class="flex flex-col gap-1">
      <span class="text-sm text-neutral-500">{{ t('transactions.form.typeLabel') }}</span>
      <AppRadioGroup :model-value="form.type"
                   :disabled="disabled"
                   :items="typeOptions"
                   orientation="horizontal"
                   @update:model-value="onTypeChange"/>
    </div>

    <UFormField :label="t('transactions.form.currency')" name="currency">
      <CurrencySelect v-model="form.currency"
                      :disabled="disabled"
                      :placeholder="t('transactions.form.currencyPlaceholder')"/>
    </UFormField>

    <BaseInput name="amount"
               v-model="form.amount"
               :max="AMOUNT_INPUT.max"
               :min="AMOUNT_INPUT.min"
               :disabled="disabled"
               :label="t('transactions.form.amount')"
               :placeholder="t('transactions.form.amountPlaceholder')"
               :trailing-text="form.currency"
               required
               type="number"/>

    <ConversionPreviewHint :foreign="previewIsForeign"
                           :amount="form.amount"
                           :converted="previewAmount"
                           :failed="previewFailed"
                           :currency="accountCurrency"/>

    <div class="flex flex-col gap-2 rounded-lg border border-default p-3">
      <div class="flex items-center justify-between gap-2">
        <div class="flex flex-col">
          <span class="text-sm font-medium">{{ t('transactions.form.split.toggle') }}</span>
          <span class="text-xs text-muted">{{ t('transactions.form.split.hint') }}</span>
        </div>
        <USwitch :model-value="isSplit"
                 :disabled="disabled || !canSplit"
                 @update:model-value="onToggleSplit"/>
      </div>

      <p v-if="!canSplit" class="text-xs text-warning">{{ t('transactions.form.split.unavailableForeign') }}</p>

      <div v-if="isSplit" class="flex flex-col gap-3 pt-1">
        <div v-for="(row, index) in form.splits"
             :key="index"
             class="flex items-end gap-2">
          <div class="flex-1 min-w-0">
            <CategorySelect :name="`split-category-${index}`"
                            v-model="row.category"
                            :disabled="disabled"
                            :options="splitCategoryOptions"
                            :label="t('transactions.form.split.category')"
                            required
                            @update:model-value="(cat) => onSplitCategoryPicked(row, cat)"/>
          </div>
          <div class="w-28 shrink-0">
            <BaseInput :name="`split-amount-${index}`"
                       v-model="row.amount"
                       :min="AMOUNT_INPUT.min"
                       :max="AMOUNT_INPUT.max"
                       :disabled="disabled"
                       :label="t('transactions.form.split.amount')"
                       :trailing-text="form.currency"
                       type="number"/>
          </div>
          <UButton color="neutral"
                   variant="ghost"
                   icon="i-lucide-x"
                   :aria-label="t('transactions.form.split.remove')"
                   :disabled="disabled"
                   class="mb-1"
                   @click="removeSplitRow(index)"/>
        </div>

        <div class="flex items-center justify-between">
          <UButton color="neutral"
                   variant="subtle"
                   size="xs"
                   icon="i-lucide-plus"
                   :disabled="disabled"
                   @click="addSplitRow">
            {{ t('transactions.form.split.add') }}
          </UButton>
          <span class="text-xs"
                :class="splitsBalanced ? 'text-success' : 'text-error'">
            <template v-if="splitsBalanced">{{ t('transactions.form.split.balanced') }}</template>
            <template v-else>
              {{ t('transactions.form.split.remaining') }}
              <BalanceNumberFormat :balance="splitRemaining" :currency="form.currency"/>
            </template>
          </span>
        </div>
      </div>
    </div>

    <BaseInput name="description"
               v-model="form.description"
               :max-length="255"
               :disabled="disabled"
               :label="t('transactions.form.description')"
               :placeholder="t('transactions.form.descriptionPlaceholder')"
               required
               type="text"/>

    <DateInput name="transactionDate"
               v-model="form.transactionDate"
               :disabled="disabled"
               :label="t('transactions.form.date')"
               required/>

    <UFormField :label="t('transactions.form.tags')">
      <div class="flex flex-wrap gap-2 mt-1">
        <button v-for="tag in tagsStore.tags"
                :key="tag.id"
                type="button"
                :disabled="disabled"
                :class="isTagSelected(tag.id) ? 'ring-2 ring-primary-500' : 'opacity-60 hover:opacity-100'"
                class="inline-flex items-center gap-1.5 rounded-full border border-default px-2.5 py-1 text-sm transition disabled:cursor-not-allowed"
                @click="toggleTag(tag.id)">
          <span class="w-2 h-2 rounded-full shrink-0" :style="{backgroundColor: tag.color}"/>
          {{ tag.name }}
        </button>
        <span v-if="tagsStore.tags.length === 0" class="text-xs text-muted">{{ t('transactions.form.noTags') }}</span>
      </div>
    </UFormField>
  </div>
</template>
