<script lang="ts" setup>
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
import {useValidator} from "~/composables/use-validator";

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

const accountSelect = ref<InstanceType<typeof AccountSelect>>();
const amountInput = ref<InstanceType<typeof BaseInput>>();
const descriptionInput = ref<InstanceType<typeof BaseInput>>();
const dateInput = ref<InstanceType<typeof DateInput>>();

const accountCurrency = computed<Currency | undefined>(() => selectedAccount.value?.currency);

// The repayment amount is in the loan's currency; show what hits the account when they differ.
const {converted: previewAmount, failed: previewFailed, isForeign: previewIsForeign} = useConversionPreview({
  accountId: computed(() => form.value.affectBalance ? form.value.accountId : undefined),
  accountCurrency: computed(() => form.value.affectBalance ? accountCurrency.value : undefined),
  amount: computed(() => Number(form.value.amount)),
  currency: computed(() => props.currency as Currency | undefined),
  date: computed(() => form.value.repaidAt),
});

const amountDescription = computed(() => {
  if (props.maxAmount === undefined) return undefined;
  const formatted = props.currency
    ? new Intl.NumberFormat(localeTag.value, {style: 'currency', currency: props.currency}).format(props.maxAmount)
    : props.maxAmount.toFixed(2);
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

defineExpose({
  validate: () => {
    const inputs = [amountInput, descriptionInput, dateInput];
    if (form.value.affectBalance) inputs.push(accountSelect);
    return useValidator().validateInputs(inputs);
  },
});
</script>

<template>
  <div class="space-y-4">
    <AppCheckbox v-model="form.affectBalance"
               :label="t('contacts.loans.repayment.form.affectBalanceLabel')"
               :description="t('contacts.loans.repayment.form.affectBalanceDescription')"/>

    <AccountSelect v-if="form.affectBalance"
                   ref="accountSelect"
                   v-model="selectedAccount"
                   :options="budgetAccountsStore.availableAccounts"
                   :description="t('contacts.loans.repayment.form.toAccountDescription')"
                   :label="t('contacts.loans.repayment.form.toAccountLabel')"
                   required/>

    <BaseInput ref="amountInput"
               v-model="form.amount"
               :description="amountDescription"
               :max="maxAmount ?? 9999999.99"
               :min="0.01"
               :label="t('contacts.loans.repayment.form.amountLabel')"
               :placeholder="t('contacts.loans.repayment.form.amountPlaceholder')"
               :trailing-text="currency"
               required
               type="number"/>

    <p v-if="previewIsForeign && Number(form.amount) > 0" class="-mt-2 px-1 text-xs text-muted">
      <span v-if="previewAmount != null && accountCurrency">
        ≈ <BalanceNumberFormat :balance="previewAmount" :currency="accountCurrency"/>
      </span>
      <span v-else-if="previewFailed">{{ t('transactions.form.conversionUnavailable') }}</span>
      <span v-else>≈ …</span>
    </p>

    <BaseInput ref="descriptionInput"
               v-model="form.description"
               :max-length="255"
               :label="t('contacts.loans.repayment.form.descriptionLabel')"
               :placeholder="t('contacts.loans.repayment.form.descriptionPlaceholder')"
               type="text"/>

    <DateInput ref="dateInput"
               v-model="form.repaidAt"
               :label="t('contacts.loans.repayment.form.dateLabel')"
               required/>
  </div>
</template>
