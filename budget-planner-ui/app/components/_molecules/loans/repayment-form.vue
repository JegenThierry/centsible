<script lang="ts" setup>
import type {RepaymentForm} from "~/models/loan/loan";
import type {BudgetAccount} from "~/models/budget-account/budget-account";
import BaseInput from "~/components/_atoms/inputs/base-input.vue";
import DateInput from "~/components/_atoms/inputs/date-input.vue";
import AccountSelect from "~/components/_atoms/inputs/account-select.vue";
import {useBudgetAccountsStore} from "~/stores/budgetAccountsStore";
import {useValidator} from "~/composables/use-validator";

const props = defineProps<{
  modelValue: RepaymentForm;
  maxAmount?: number;
}>();

const emit = defineEmits(['update:modelValue']);

const budgetAccountsStore = useBudgetAccountsStore();
const {t} = useI18n();

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

const amountDescription = computed(() => {
  if (props.maxAmount === undefined) return undefined;
  return t('contacts.loans.repayment.form.outstandingHelp', {amount: props.maxAmount.toFixed(2)});
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
    <UCheckbox v-model="form.affectBalance"
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
               required
               type="number"/>

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
