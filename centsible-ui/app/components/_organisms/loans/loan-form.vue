<script lang="ts" setup>
import type {LoanForm} from "~/models/loan/loan";
import type {Contact} from "~/models/contact/contact";
import type {BudgetAccount} from "~/models/budget-account/budget-account";
import AccountSelect from "~/components/_atoms/inputs/account-select.vue";
import CurrencySelect from "~/components/_atoms/inputs/currency-select.vue";
import AppCheckbox from "~/components/_atoms/ui/app-checkbox.vue";
import LoanContactModeFields from "~/components/_molecules/loans/loan-contact-mode-fields.vue";
import LoanAmountFields from "~/components/_molecules/loans/loan-amount-fields.vue";
import BalanceNumberFormat from "~/components/_atoms/labels/balance-number-format.vue";
import {Currency} from "~/models/budget-account/currency";
import {useConversionPreview} from "~/composables/use-conversion-preview";
import {useContactsStore} from "~/stores/contactsStore";
import {useBudgetAccountsStore} from "~/stores/budgetAccountsStore";
import {useUserStore} from "~/stores/userStore";

const props = defineProps<{
  modelValue: LoanForm;
  lockContact?: boolean;
  disabled?: boolean;
}>();

const emit = defineEmits(['update:modelValue']);

const contactsStore = useContactsStore();
const budgetAccountsStore = useBudgetAccountsStore();
const userStore = useUserStore();
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

function defaultCurrency(): Currency {
  if (form.value.affectBalance && selectedAccount.value) return selectedAccount.value.currency;
  return userStore.user?.defaultCurrency ?? Currency.EUR;
}

// Currency defaults to the account currency (balance-affecting) or the user's default, but can be
// overridden to record a loan in a foreign currency (the backend converts the linked transaction).
const currency = computed<Currency>({
  get: () => form.value.currency ?? defaultCurrency(),
  set: (c) => { form.value = {...form.value, currency: c}; },
});

const amountCurrency = computed(() => currency.value);

// When a balance-affecting loan is in a foreign currency, show what actually leaves the account.
const accountCurrency = computed<Currency | undefined>(() => selectedAccount.value?.currency);
const {converted: previewAmount, failed: previewFailed, isForeign: previewIsForeign} = useConversionPreview({
  accountId: computed(() => form.value.affectBalance ? form.value.accountId : undefined),
  accountCurrency: computed(() => form.value.affectBalance ? accountCurrency.value : undefined),
  amount: computed(() => Number(form.value.lentAmount)),
  currency: computed(() => currency.value),
  date: computed(() => form.value.transactionDate),
});

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
</script>

<template>
  <div class="space-y-4">
    <LoanContactModeFields v-model="form"
                           v-model:mode="mode"
                           v-model:selected-contact="selectedContact"
                           :contacts="contactsStore.contacts"
                           :disabled="disabled"
                           :lock-contact="lockContact"/>

    <AppCheckbox v-model="form.affectBalance"
                 :disabled="disabled"
                 :label="t('contacts.loans.form.affectBalanceLabel')"
                 :description="t('contacts.loans.form.affectBalanceDescription')"/>

    <AccountSelect v-if="form.affectBalance"
                   name="accountId"
                   v-model="selectedAccount"
                   :disabled="disabled"
                   :options="budgetAccountsStore.availableAccounts"
                   :description="t('contacts.loans.form.fromAccountDescription')"
                   :label="t('contacts.loans.form.fromAccountLabel')"
                   required/>

    <UFormField :label="t('contacts.loans.form.currencyLabel')"
                :description="t('contacts.loans.form.currencyDescription')">
      <CurrencySelect v-model="currency" :disabled="disabled"/>
    </UFormField>

    <LoanAmountFields v-model="form"
                      :currency="amountCurrency"
                      :disabled="disabled"/>

    <p v-if="previewIsForeign && Number(form.lentAmount) > 0" class="-mt-2 px-1 text-xs text-muted">
      <span v-if="previewAmount != null && accountCurrency">
        ≈ <BalanceNumberFormat :balance="previewAmount" :currency="accountCurrency"/>
      </span>
      <span v-else-if="previewFailed">{{ t('transactions.form.conversionUnavailable') }}</span>
      <span v-else>≈ …</span>
    </p>
  </div>
</template>
