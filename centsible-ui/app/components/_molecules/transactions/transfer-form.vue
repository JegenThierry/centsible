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

const amountInput = ref<InstanceType<typeof BaseInput>>();
const descriptionInput = ref<InstanceType<typeof BaseInput>>();
const dateInput = ref<InstanceType<typeof DateInput>>();
const sourceInput = ref<InstanceType<typeof AccountSelect>>();
const destinationInput = ref<InstanceType<typeof AccountSelect>>();

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

const {converted: previewAmount, failed: previewFailed, isForeign: previewIsForeign} = useConversionPreview({
  accountId: computed(() => form.value.destinationAccountId),
  accountCurrency: destinationCurrency,
  amount: computed(() => form.value.amount),
  currency: sourceCurrency,
  date: computed(() => form.value.transactionDate),
});

defineExpose({
  validate: () => useValidator().validateInputs([sourceInput, destinationInput, amountInput, descriptionInput, dateInput]),
});
</script>

<template>
  <div class="space-y-4">
    <AccountSelect ref="sourceInput"
                   v-model="sourceAccount"
                   :options="accounts"
                   :label="t('transactions.transfer.fromAccount')"
                   :description="t('transactions.transfer.fromAccountHelp')"
                   :disabled="disabled || sourceLocked"
                   required/>

    <AccountSelect ref="destinationInput"
                   v-model="destinationAccount"
                   :options="accounts"
                   :label="t('transactions.transfer.toAccount')"
                   :description="t('transactions.transfer.toAccountHelp')"
                   :disabled="disabled"
                   required/>

    <BaseInput ref="amountInput"
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

    <BaseInput ref="descriptionInput"
               v-model="form.description"
               :max-length="255"
               :disabled="disabled"
               :label="t('transactions.form.description')"
               :placeholder="t('transactions.form.descriptionPlaceholder')"
               required
               type="text"/>

    <DateInput ref="dateInput"
               v-model="form.transactionDate"
               :disabled="disabled"
               :label="t('transactions.form.date')"
               required/>
  </div>
</template>
