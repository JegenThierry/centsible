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
               label="Add to an account"
               description="Uncheck if the money was received outside your tracked accounts (e.g. handed back in cash) and you only want to mark it repaid."/>

    <AccountSelect v-if="form.affectBalance"
                   ref="accountSelect"
                   v-model="selectedAccount"
                   :options="budgetAccountsStore.availableAccounts"
                   description="The account the money is paid back into."
                   label="To account"
                   required/>

    <BaseInput ref="amountInput"
               v-model="form.amount"
               :description="maxAmount !== undefined ? `Outstanding: ${maxAmount.toFixed(2)}` : undefined"
               :max="maxAmount ?? 9999999.99"
               :min="0.01"
               label="Amount"
               placeholder="0.00"
               required
               type="number"/>

    <BaseInput ref="descriptionInput"
               v-model="form.description"
               :max-length="255"
               label="Description (optional)"
               placeholder="Repayment"
               type="text"/>

    <DateInput ref="dateInput"
               v-model="form.repaidAt"
               label="Repayment date"
               required/>
  </div>
</template>
