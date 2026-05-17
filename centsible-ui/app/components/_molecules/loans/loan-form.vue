<script lang="ts" setup>
import type {LoanForm} from "~/models/loan/loan";
import type {Contact} from "~/models/contact/contact";
import type {BudgetAccount} from "~/models/budget-account/budget-account";
import BaseInput from "~/components/_atoms/inputs/base-input.vue";
import DateInput from "~/components/_atoms/inputs/date-input.vue";
import ContactSelect from "~/components/_atoms/inputs/contact-select.vue";
import AccountSelect from "~/components/_atoms/inputs/account-select.vue";
import {useContactsStore} from "~/stores/contactsStore";
import {useBudgetAccountsStore} from "~/stores/budgetAccountsStore";
import {useValidator} from "~/composables/use-validator";

const props = defineProps<{
  modelValue: LoanForm;
  lockContact?: boolean;
  disabled?: boolean;
}>();

const emit = defineEmits(['update:modelValue']);

const contactsStore = useContactsStore();
const budgetAccountsStore = useBudgetAccountsStore();
const {t} = useI18n();

const form = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val),
});

const mode = ref<'existing' | 'new'>('existing');

const selectedContact = computed<Contact | undefined>({
  get: () => form.value.contactId ? contactsStore.findContactById(form.value.contactId) : undefined,
  set: (c) => { form.value = {...form.value, contactId: c?.id}; },
});

const selectedAccount = computed<BudgetAccount | undefined>({
  get: () => budgetAccountsStore.availableAccounts.find(a => a.id === form.value.accountId),
  set: (a) => { form.value = {...form.value, accountId: a?.id}; },
});

const amountCurrency = computed(() => selectedAccount.value?.currency);

const contactSelect = ref<InstanceType<typeof ContactSelect>>();
const firstNameInput = ref<InstanceType<typeof BaseInput>>();
const lastNameInput = ref<InstanceType<typeof BaseInput>>();
const accountSelect = ref<InstanceType<typeof AccountSelect>>();
const lentInput = ref<InstanceType<typeof BaseInput>>();
const owedInput = ref<InstanceType<typeof BaseInput>>();
const descriptionInput = ref<InstanceType<typeof BaseInput>>();
const dateInput = ref<InstanceType<typeof DateInput>>();

const modeOptions = computed(() => [
  {label: t('contacts.loans.form.modeExisting'), value: 'existing'},
  {label: t('contacts.loans.form.modeNew'), value: 'new'},
]);

watch(mode, (m) => {
  if (m === 'existing') {
    form.value = {...form.value, newContactFirstName: undefined, newContactLastName: undefined};
  } else {
    form.value = {...form.value, contactId: undefined};
  }
});

watch(() => form.value.lentAmount, (newLent, oldLent) => {
  if (Number(form.value.owedAmount) === Number(oldLent ?? 0)) {
    form.value = {...form.value, owedAmount: newLent};
  }
});

onMounted(async () => {
  const tasks: Promise<unknown>[] = [];
  if (contactsStore.contacts.length === 0) tasks.push(contactsStore.updateContacts());
  if (budgetAccountsStore.availableAccounts.length === 0) tasks.push(budgetAccountsStore.updateAvailableAccounts());
  await Promise.all(tasks);

  if (!props.lockContact && contactsStore.contacts.length === 0) {
    mode.value = 'new';
  }
  if (!form.value.accountId && budgetAccountsStore.activeAccount) {
    form.value = {...form.value, accountId: budgetAccountsStore.activeAccount.id};
  }
});

function validate(): boolean {
  const inputs = [lentInput, owedInput, descriptionInput, dateInput];
  if (form.value.affectBalance) inputs.push(accountSelect);
  if (mode.value === 'existing') inputs.push(contactSelect);
  else inputs.push(firstNameInput);
  return useValidator().validateInputs(inputs);
}

defineExpose({validate});
</script>

<template>
  <div class="space-y-4">
    <URadioGroup v-if="!lockContact"
                 v-model="mode"
                 :disabled="disabled"
                 :items="modeOptions"
                 :legend="t('contacts.loans.form.modeLegend')"
                 orientation="horizontal"/>

    <ContactSelect v-if="mode === 'existing' && !lockContact"
                   ref="contactSelect"
                   v-model="selectedContact"
                   :disabled="disabled"
                   :options="contactsStore.contacts"
                   :label="t('contacts.loans.form.pickContact')"
                   required/>

    <template v-if="mode === 'new' && !lockContact">
      <BaseInput ref="firstNameInput"
                 v-model="form.newContactFirstName"
                 :max-length="100"
                 :description="t('contacts.loans.form.newFirstNameDescription')"
                 :disabled="disabled"
                 :label="t('contacts.loans.form.newFirstNameLabel')"
                 :placeholder="t('contacts.loans.form.newFirstNamePlaceholder')"
                 required
                 type="text"/>
      <BaseInput ref="lastNameInput"
                 v-model="form.newContactLastName"
                 :max-length="100"
                 :disabled="disabled"
                 :label="t('contacts.loans.form.newLastNameLabel')"
                 :placeholder="t('contacts.loans.form.newLastNamePlaceholder')"
                 type="text"/>
    </template>

    <UCheckbox v-model="form.affectBalance"
               :disabled="disabled"
               :label="t('contacts.loans.form.affectBalanceLabel')"
               :description="t('contacts.loans.form.affectBalanceDescription')"/>

    <AccountSelect v-if="form.affectBalance"
                   ref="accountSelect"
                   v-model="selectedAccount"
                   :disabled="disabled"
                   :options="budgetAccountsStore.availableAccounts"
                   :description="t('contacts.loans.form.fromAccountDescription')"
                   :label="t('contacts.loans.form.fromAccountLabel')"
                   required/>

    <BaseInput ref="lentInput"
               v-model="form.lentAmount"
               :max="9999999.99"
               :min="0.01"
               :disabled="disabled"
               :label="t('contacts.loans.form.lentLabel')"
               :placeholder="t('contacts.loans.form.lentPlaceholder')"
               :trailing-text="amountCurrency"
               required
               type="number"/>

    <BaseInput ref="owedInput"
               v-model="form.owedAmount"
               :max="9999999.99"
               :min="0"
               :description="t('contacts.loans.form.owedDescription')"
               :disabled="disabled"
               :label="t('contacts.loans.form.owedLabel')"
               :placeholder="t('contacts.loans.form.owedPlaceholder')"
               :trailing-text="amountCurrency"
               required
               type="number"/>

    <BaseInput ref="descriptionInput"
               v-model="form.description"
               :max-length="255"
               :disabled="disabled"
               :label="t('contacts.loans.form.descriptionLabel')"
               :placeholder="t('contacts.loans.form.descriptionPlaceholder')"
               required
               type="text"/>

    <DateInput ref="dateInput"
               v-model="form.transactionDate"
               :disabled="disabled"
               :label="t('contacts.loans.form.dateLabel')"
               required/>
  </div>
</template>
