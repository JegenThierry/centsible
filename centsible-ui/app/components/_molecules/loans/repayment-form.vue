<script lang="ts" setup>
import {MONEY_FIELD_MAX} from "~/utils/money";
import type {RepaymentForm} from "~/models/loan/loan";
import type {BudgetAccount} from "~/models/budget-account/budget-account";
import BaseInput from "~/components/_atoms/inputs/base-input.vue";
import DateInput from "~/components/_atoms/inputs/date-input.vue";
import AccountSelect from "~/components/_atoms/inputs/account-select.vue";
import AppCheckbox from "~/components/_atoms/ui/app-checkbox.vue";
import BalanceNumberFormat from "~/components/_atoms/labels/balance-number-format.vue";
import type {Currency} from "~/models/budget-account/currency";
import {useBudgetAccountsStore} from "~/stores/budgetAccountsStore";
import {useConversionPreview} from "~/composables/use-conversion-preview";

const props = defineProps<{
  modelValue: RepaymentForm;
  maxAmount?: number;
  currency?: string;
}>();

const emit = defineEmits(['update:modelValue']);

const budgetAccountsStore = useBudgetAccountsStore();
const {t} = useI18n();
const localeTag = useLocaleTag();

const form = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val),
});

const selectedAccount = computed<BudgetAccount | undefined>({
  get: () => budgetAccountsStore.availableAccounts.find(a => a.id === form.value.accountId),
  set: (a) => { form.value = {...form.value, accountId: a?.id}; },
});

const accountCurrency = computed<Currency | undefined>(() => selectedAccount.value?.currency);

const {converted: previewAmount, failed: previewFailed, isForeign: previewIsForeign} = useConversionPreview({
  accountId: computed(() => form.value.affectBalance ? form.value.accountId : undefined),
  accountCurrency: computed(() => form.value.affectBalance ? accountCurrency.value : undefined),
  amount: computed(() => Number(form.value.amount)),
  currency: computed(() => props.currency as Currency | undefined),
  date: computed(() => form.value.repaidAt),
});

const amountDescription = computed(() => {
  if (props.maxAmount === undefined) return undefined;
  const formatted = formatCurrency(props.maxAmount, props.currency, localeTag.value);
  return t('contacts.loans.repayment.form.outstandingHelp', {amount: formatted});
});

onMounted(async () => {
  if (budgetAccountsStore.availableAccounts.length === 0) {
    await budgetAccountsStore.updateAvailableAccounts();
  }
  if (!form.value.accountId && budgetAccountsStore.activeAccount) {
    form.value = {...form.value, accountId: budgetAccountsStore.activeAccount.id};
  }
});
</script>

<template>
  <div class="space-y-4">
    <AppCheckbox v-model="form.affectBalance"
               :label="t('contacts.loans.repayment.form.affectBalanceLabel')"
               :description="t('contacts.loans.repayment.form.affectBalanceDescription')"/>

    <AccountSelect v-if="form.affectBalance"
                   name="accountId"
                   v-model="selectedAccount"
                   :options="budgetAccountsStore.availableAccounts"
                   :description="t('contacts.loans.repayment.form.toAccountDescription')"
                   :label="t('contacts.loans.repayment.form.toAccountLabel')"
                   required/>

    <BaseInput name="amount"
               v-model="form.amount"
               :description="amountDescription"
               :max="maxAmount ?? MONEY_FIELD_MAX"
               :min="0.01"
               :label="t('contacts.loans.repayment.form.amountLabel')"
               :placeholder="t('contacts.loans.repayment.form.amountPlaceholder')"
               :trailing-text="currency"
               required
               type="number"/>

    <ConversionPreviewHint :foreign="previewIsForeign"
                           :amount="Number(form.amount)"
                           :converted="previewAmount"
                           :failed="previewFailed"
                           :currency="accountCurrency"/>

    <BaseInput name="description"
               v-model="form.description"
               :max-length="255"
               :label="t('contacts.loans.repayment.form.descriptionLabel')"
               :placeholder="t('contacts.loans.repayment.form.descriptionPlaceholder')"
               type="text"/>

    <DateInput name="repaidAt"
               v-model="form.repaidAt"
               :label="t('contacts.loans.repayment.form.dateLabel')"
               required/>
  </div>
</template>
