<script lang="ts" setup>
import adze from 'adze'
import {z} from 'zod'
import type {FormSubmitEvent} from '@nuxt/ui'
import {type LoanForm as LoanFormModel, cleanOptionalNumber} from "~/models/loan/loan";
import LoanForm from "~/components/_organisms/loans/loan-form.vue";
import ModalFooterActions from "~/components/_molecules/modals/modal-footer-actions.vue";
import {useLoansStore} from "~/stores/loansStore";
import {todayIsoDate} from "~/utils/date";

const props = defineProps<{
  contactId?: string;
}>();

const isOpen = defineModel<boolean>('open', {required: true});

const emit = defineEmits<{
  (e: 'created'): void;
}>();

const loansStore = useLoansStore();
const {t} = useI18n();

const form = ref<LoanFormModel>(makeBlankForm());
const loading = ref(false);
const formId = useId();

const contactLabel = t('contacts.loans.form.pickContact');
const firstNameLabel = t('contacts.loans.form.newFirstNameLabel');
const accountLabel = t('contacts.loans.form.fromAccountLabel');
const lentLabel = t('contacts.loans.form.lentLabel');
const owedLabel = t('contacts.loans.form.owedLabel');
const interestLabel = t('contacts.loans.form.interestRateLabel');
const descriptionLabel = t('contacts.loans.form.descriptionLabel');
const dateLabel = t('contacts.loans.form.dateLabel');
const notesLabel = t('contacts.loans.form.notesLabel');
const lastNameLabel = t('contacts.loans.form.newLastNameLabel');

const schema = z.object({
  contactId: z.string().optional(),
  newContactFirstName: z.string().trim()
    .max(100, t('common.validation.maxLength', {field: firstNameLabel, max: 100}))
    .optional(),
  newContactLastName: z.string().trim()
    .max(100, t('common.validation.maxLength', {field: lastNameLabel, max: 100}))
    .optional(),
  affectBalance: z.boolean(),
  accountId: z.string().optional(),
  lentAmount: z.coerce.number({message: t('common.validation.number', {field: lentLabel})})
    .min(0.01, t('common.validation.min', {field: lentLabel, min: 0.01}))
    .max(9999999.99, t('common.validation.max', {field: lentLabel, max: 9999999.99})),
  interestRate: z.coerce.number({message: t('common.validation.number', {field: interestLabel})})
    .min(0, t('common.validation.min', {field: interestLabel, min: 0}))
    .max(999.99, t('common.validation.max', {field: interestLabel, max: 999.99}))
    .optional(),
  owedAmount: z.coerce.number({message: t('common.validation.number', {field: owedLabel})})
    .min(0, t('common.validation.min', {field: owedLabel, min: 0}))
    .max(9999999.99, t('common.validation.max', {field: owedLabel, max: 9999999.99})),
  description: z.string().trim()
    .min(1, t('common.validation.required', {field: descriptionLabel}))
    .max(255, t('common.validation.maxLength', {field: descriptionLabel, max: 255})),
  transactionDate: z.string().min(1, t('common.validation.required', {field: dateLabel})),
  dueDate: z.string().optional(),
  notes: z.string().trim()
    .max(500, t('common.validation.maxLength', {field: notesLabel, max: 500}))
    .optional(),
}).superRefine((d, ctx) => {
  // A contact is required: either an existing one (contactId, also set when lockContact) or a new
  // contact's first name. The error lands on whichever field the active mode shows.
  if (!d.contactId && !d.newContactFirstName) {
    ctx.addIssue({code: z.ZodIssueCode.custom, path: ['contactId'], message: t('common.validation.required', {field: contactLabel})});
    ctx.addIssue({code: z.ZodIssueCode.custom, path: ['newContactFirstName'], message: t('common.validation.required', {field: firstNameLabel})});
  }
  if (d.affectBalance && !d.accountId) {
    ctx.addIssue({code: z.ZodIssueCode.custom, path: ['accountId'], message: t('common.validation.required', {field: accountLabel})});
  }
});
type Schema = z.output<typeof schema>;

function makeBlankForm(): LoanFormModel {
  return {
    contactId: props.contactId,
    newContactFirstName: undefined,
    newContactLastName: undefined,
    accountId: undefined,
    affectBalance: true,
    lentAmount: 0,
    owedAmount: 0,
    currency: undefined,
    interestRate: undefined,
    description: '',
    transactionDate: todayIsoDate(),
    dueDate: undefined,
    notes: undefined,
  };
}

watch(isOpen, (open) => {
  if (open) form.value = makeBlankForm();
});

async function handleSave(_event: FormSubmitEvent<Schema>) {
  loading.value = true;
  try {
    await loansStore.createLoan({...form.value, interestRate: cleanOptionalNumber(form.value.interestRate)});
    emit('created');
    isOpen.value = false;
  } catch (error) {
    adze.ns('loans').error('Create loan failed', error);
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <UModal v-model:open="isOpen"
          :description="t('contacts.loans.create.description')"
          :title="t('contacts.loans.create.title')">
    <template #body>
      <UForm :id="formId" :schema="schema" :state="form" @submit="handleSave">
        <LoanForm v-model="form" :lock-contact="!!contactId"/>
      </UForm>
    </template>

    <template #footer>
      <ModalFooterActions :form="formId"
                          :loading="loading"
                          :submit-label="t('contacts.loans.create.submit')"
                          @cancel="isOpen = false"/>
    </template>
  </UModal>
</template>
