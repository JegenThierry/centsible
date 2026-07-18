<script lang="ts" setup>
import {MONEY_FIELD_MAX} from "~/utils/money";
import adze from 'adze'
import {z} from 'zod'
import type {FormSubmitEvent} from '@nuxt/ui'
import {type Loan, type LoanUpdateForm, cleanOptionalNumber, computeOwedFromLent, hasInterestRate} from "~/models/loan/loan";
import BaseInput from "~/components/_atoms/inputs/base-input.vue";
import DateInput from "~/components/_atoms/inputs/date-input.vue";
import FormModal from "~/components/_molecules/modals/form-modal.vue";
import {useLoansStore} from "~/stores/loansStore";

const props = defineProps<{
  loan?: Loan;
}>();

const isOpen = defineModel<boolean>('open', {required: true});

const emit = defineEmits<{
  (e: 'updated'): void;
}>();

const loansStore = useLoansStore();
const {t} = useI18n();

const form = ref<LoanUpdateForm>(makeForm());
const loading = ref(false);

const owedLabel = t('contacts.loans.form.owedLabel');
const interestLabel = t('contacts.loans.form.interestRateLabel');
const descriptionLabel = t('contacts.loans.form.descriptionLabel');
const notesLabel = t('contacts.loans.form.notesLabel');

const schema = z.object({
  description: z.string().trim()
    .min(1, t('common.validation.required', {field: descriptionLabel}))
    .max(255, t('common.validation.maxLength', {field: descriptionLabel, max: 255})),
  interestRate: z.coerce.number({message: t('common.validation.number', {field: interestLabel})})
    .min(0, t('common.validation.min', {field: interestLabel, min: 0}))
    .max(999.99, t('common.validation.max', {field: interestLabel, max: 999.99}))
    .optional(),
  owedAmount: z.coerce.number({message: t('common.validation.number', {field: owedLabel})})
    .min(0, t('common.validation.min', {field: owedLabel, min: 0}))
    .max(MONEY_FIELD_MAX, t('common.validation.max', {field: owedLabel, max: MONEY_FIELD_MAX})),
  dueDate: z.string().optional(),
  notes: z.string().trim()
    .max(500, t('common.validation.maxLength', {field: notesLabel, max: 500}))
    .optional(),
})
type Schema = z.output<typeof schema>

function makeForm(): LoanUpdateForm {
  const l = props.loan;
  return {
    description: l?.description ?? '',
    owedAmount: Number(l?.owedAmount ?? 0),
    interestRate: l?.interestRate ?? undefined,
    dueDate: l?.dueDate ?? undefined,
    notes: l?.notes ?? undefined,
  };
}

const getSnapshot = () => form.value;
const onResetOnOpen = () => {
  form.value = makeForm();
};

const hasInterest = computed(() => hasInterestRate(form.value.interestRate));

watch(() => form.value.interestRate, () => {
  if (!hasInterest.value || !props.loan) return;
  const owed = computeOwedFromLent(props.loan.lentAmount, form.value.interestRate);
  if (Number(form.value.owedAmount) !== owed) form.value = {...form.value, owedAmount: owed};
});

async function handleSave(_event: FormSubmitEvent<Schema>) {
  if (!props.loan) return;

  loading.value = true;
  try {
    await loansStore.updateLoan(
      props.loan.id,
      props.loan.contact.id,
      {...form.value, interestRate: cleanOptionalNumber(form.value.interestRate)},
    );
    emit('updated');
    isOpen.value = false;
  } catch (error) {
    adze.ns('loans').error('Update loan failed', error);
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <FormModal v-model="isOpen"
             :description="t('contacts.loans.edit.description')"
             :title="t('contacts.loans.edit.title')"
             :schema="schema"
             :state="form"
             :loading="loading"
             :get-snapshot="getSnapshot"
             :on-reset-on-open="onResetOnOpen"
             :submit-label="t('contacts.loans.edit.submit')"
             @submit="handleSave">
    <template #fields>
      <BaseInput name="description"
                 v-model="form.description"
                 :max-length="255"
                 :label="t('contacts.loans.form.descriptionLabel')"
                 :placeholder="t('contacts.loans.form.descriptionPlaceholder')"
                 required
                 type="text"/>

      <BaseInput name="interestRate"
                 v-model="form.interestRate"
                 :max="999.99"
                 :min="0"
                 :label="t('contacts.loans.form.interestRateLabel')"
                 :description="t('contacts.loans.form.interestRateDescription')"
                 :placeholder="t('contacts.loans.form.interestRatePlaceholder')"
                 trailing-text="%"
                 type="number"/>

      <BaseInput name="owedAmount"
                 v-model="form.owedAmount"
                 :max="MONEY_FIELD_MAX"
                 :min="0"
                 :description="hasInterest ? t('contacts.loans.form.owedComputedDescription') : t('contacts.loans.form.owedDescription')"
                 :disabled="hasInterest"
                 :label="t('contacts.loans.form.owedLabel')"
                 :placeholder="t('contacts.loans.form.owedPlaceholder')"
                 :trailing-text="loan?.currency"
                 required
                 type="number"/>

      <DateInput name="dueDate"
                 v-model="form.dueDate"
                 :label="t('contacts.loans.form.dueDateLabel')"/>

      <BaseInput name="notes"
                 v-model="form.notes"
                 :max-length="500"
                 :label="t('contacts.loans.form.notesLabel')"
                 :placeholder="t('contacts.loans.form.notesPlaceholder')"
                 type="text"/>
    </template>
  </FormModal>
</template>
