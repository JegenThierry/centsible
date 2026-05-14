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
}>();

const emit = defineEmits(['update:modelValue']);

const contactsStore = useContactsStore();
const budgetAccountsStore = useBudgetAccountsStore();

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

const contactSelect = ref<InstanceType<typeof ContactSelect>>();
const firstNameInput = ref<InstanceType<typeof BaseInput>>();
const lastNameInput = ref<InstanceType<typeof BaseInput>>();
const accountSelect = ref<InstanceType<typeof AccountSelect>>();
const lentInput = ref<InstanceType<typeof BaseInput>>();
const owedInput = ref<InstanceType<typeof BaseInput>>();
const descriptionInput = ref<InstanceType<typeof BaseInput>>();
const dateInput = ref<InstanceType<typeof DateInput>>();

const modeOptions = [
  {label: 'Existing contact', value: 'existing'},
  {label: 'New person', value: 'new'},
];

watch(mode, (m) => {
  if (m === 'existing') {
    form.value = {...form.value, newContactFirstName: undefined, newContactLastName: undefined};
  } else {
    form.value = {...form.value, contactId: undefined};
  }
});

// Mirror lent → owed only while the user hasn't customised owed (owed still matches the prior lent).
watch(() => form.value.lentAmount, (newLent, oldLent) => {
  if (Number(form.value.owedAmount) === Number(oldLent ?? 0)) {
    form.value = {...form.value, owedAmount: newLent};
  }
});

onMounted(async () => {
  await Promise.all([
    contactsStore.contacts.length === 0 ? contactsStore.updateContacts() : Promise.resolve(),
    budgetAccountsStore.availableAccounts.length === 0
      ? budgetAccountsStore.updateAvailableAccounts()
      : Promise.resolve(),
  ]);

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
    <div v-if="!lockContact">
      <URadioGroup v-model="mode"
                   :items="modeOptions"
                   legend="Contact"
                   orientation="horizontal"/>
    </div>

    <ContactSelect v-if="mode === 'existing' && !lockContact"
                   ref="contactSelect"
                   v-model="selectedContact"
                   :options="contactsStore.contacts"
                   label="Pick a contact"
                   required/>

    <template v-if="mode === 'new' && !lockContact">
      <BaseInput ref="firstNameInput"
                 v-model="form.newContactFirstName"
                 :max-length="100"
                 description="A contact will be created and the loan attached to it."
                 label="First name"
                 placeholder="e.g. Alex"
                 required
                 type="text"/>
      <BaseInput ref="lastNameInput"
                 v-model="form.newContactLastName"
                 :max-length="100"
                 label="Last name (optional)"
                 placeholder="e.g. Smith"
                 type="text"/>
    </template>

    <UCheckbox v-model="form.affectBalance"
               label="Deduct from an account"
               description="Uncheck if the money already left your tracked accounts (e.g. handed over in cash) and you only want to track the loan."/>

    <AccountSelect v-if="form.affectBalance"
                   ref="accountSelect"
                   v-model="selectedAccount"
                   :options="budgetAccountsStore.availableAccounts"
                   description="The account the money leaves from."
                   label="From account"
                   required/>

    <BaseInput ref="lentInput"
               v-model="form.lentAmount"
               :max="9999999.99"
               :min="0.01"
               label="Lent amount"
               placeholder="0.00"
               required
               type="number"/>

    <BaseInput ref="owedInput"
               v-model="form.owedAmount"
               :max="9999999.99"
               :min="0"
               description="Defaults to the lent amount. Increase it if they owe you more (e.g. interest)."
               label="Amount owed back"
               placeholder="0.00"
               required
               type="number"/>

    <BaseInput ref="descriptionInput"
               v-model="form.description"
               :max-length="255"
               label="Description"
               placeholder="What was the loan for?"
               required
               type="text"/>

    <DateInput ref="dateInput"
               v-model="form.transactionDate"
               label="Date"
               required/>
  </div>
</template>
