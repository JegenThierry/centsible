<script lang="ts" setup>
import type {BudgetAccount} from "~/models/budget-account/budget-account";
import type {TransferForm} from "~/models/transactions/transaction";
import type {Currency} from "~/models/budget-account/currency";
import AccountSelect from "~/components/_atoms/inputs/account-select.vue";
import BaseInput from "~/components/_atoms/inputs/base-input.vue";
import DateInput from "~/components/_atoms/inputs/date-input.vue";
import BalanceNumberFormat from "~/components/_atoms/labels/balance-number-format.vue";
import {useConversionPreview} from "~/composables/use-conversion-preview";
import {AMOUNT_INPUT} from "~/utils/money";

const props = defineProps<{
  accounts: BudgetAccount[];
  disabled?: boolean;
  sourceLocked?: boolean;
}>();

const form = defineModel<TransferForm>({required: true});
const {t} = useI18n();

const sourceAccount = computed<BudgetAccount | undefined>({
  get: () => props.accounts.find(a => a.id === form.value.sourceAccountId),
  set: (account) => { form.value.sourceAccountId = account?.id; },
});
const destinationAccount = computed<BudgetAccount | undefined>({
  get: () => props.accounts.find(a => a.id === form.value.destinationAccountId),
  set: (account) => { form.value.destinationAccountId = account?.id; },
});

const sourceCurrency = computed<Currency | undefined>(() => sourceAccount.value?.currency);
const destinationCurrency = computed<Currency | undefined>(() => destinationAccount.value?.currency);

const sourceOptions = computed(() => props.accounts.filter(a => a.id !== form.value.destinationAccountId));
const destinationOptions = computed(() => props.accounts.filter(a => a.id !== form.value.sourceAccountId));

const {converted: previewAmount, failed: previewFailed, isForeign: previewIsForeign} = useConversionPreview({
  accountId: computed(() => form.value.destinationAccountId),
  accountCurrency: destinationCurrency,
  amount: computed(() => form.value.amount),
  currency: sourceCurrency,
  date: computed(() => form.value.transactionDate),
});
</script>

<template>
  <div class="space-y-4">
    <AccountSelect name="sourceAccountId"
                   v-model="sourceAccount"
                   :options="sourceOptions"
                   :label="t('transactions.transfer.fromAccount')"
                   :description="t('transactions.transfer.fromAccountHelp')"
                   :disabled="disabled || sourceLocked"
                   required/>

    <AccountSelect name="destinationAccountId"
                   v-model="destinationAccount"
                   :options="destinationOptions"
                   :label="t('transactions.transfer.toAccount')"
                   :description="t('transactions.transfer.toAccountHelp')"
                   :disabled="disabled"
                   required/>

    <BaseInput name="amount"
               v-model="form.amount"
               :max="AMOUNT_INPUT.max"
               :min="AMOUNT_INPUT.min"
               :disabled="disabled"
               :label="t('transactions.transfer.amount')"
               :placeholder="t('transactions.form.amountPlaceholder')"
               :trailing-text="sourceCurrency"
               required
               type="number"/>

    <p v-if="previewIsForeign && form.amount > 0" class="-mt-2 px-1 text-xs text-neutral-400">
      <span v-if="previewAmount != null && destinationCurrency">
        ≈ <BalanceNumberFormat :balance="previewAmount" :currency="destinationCurrency"/>
      </span>
      <span v-else-if="previewFailed">{{ t('transactions.form.conversionUnavailable') }}</span>
      <span v-else>≈ …</span>
    </p>

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
  </div>
</template>
