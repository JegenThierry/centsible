<script lang="ts" setup>
import {MONEY_FIELD_MAX} from "~/utils/money";
import adze from 'adze'
import {z} from 'zod'
import type {FormSubmitEvent} from '@nuxt/ui'
import type {Loan, RepaymentForm as RepaymentFormModel} from "~/models/loan/loan";
import RepaymentForm from "~/components/_molecules/loans/repayment-form.vue";
import FormModal from "~/components/_molecules/modals/form-modal.vue";
import {useLoansStore} from "~/stores/loansStore";
import {todayIsoDate} from "~/utils/date";

const props = defineProps<{
  loan: Loan | undefined;
}>();

const isOpen = defineModel<boolean>('open', {required: true});

const emit = defineEmits<{
  (e: 'recorded'): void;
}>();

const loansStore = useLoansStore();
const {t} = useI18n();

const form = ref<RepaymentFormModel>(makeBlankForm());
const loading = ref(false);

const accountLabel = t('contacts.loans.repayment.form.toAccountLabel');
const amountLabel = t('contacts.loans.repayment.form.amountLabel');
const descriptionLabel = t('contacts.loans.repayment.form.descriptionLabel');
const dateLabel = t('contacts.loans.repayment.form.dateLabel');

const maxAmount = computed(() => Number(props.loan?.outstanding ?? MONEY_FIELD_MAX));

const schema = computed(() => z.object({
  affectBalance: z.boolean(),
  accountId: z.string().optional(),
  amount: z.coerce.number({message: t('common.validation.number', {field: amountLabel})})
    .min(0.01, t('common.validation.min', {field: amountLabel, min: 0.01}))
    .max(maxAmount.value, t('common.validation.max', {field: amountLabel, max: maxAmount.value})),
  description: z.string().trim()
    .max(255, t('common.validation.maxLength', {field: descriptionLabel, max: 255}))
    .optional(),
  repaidAt: z.string().min(1, t('common.validation.required', {field: dateLabel})),
}).superRefine((d, ctx) => {
  if (d.affectBalance && !d.accountId) {
    ctx.addIssue({code: z.ZodIssueCode.custom, path: ['accountId'], message: t('common.validation.required', {field: accountLabel})});
  }
}));
type Schema = z.output<typeof schema.value>;

const description = computed(() => {
  if (!props.loan) return '';
  return t('contacts.loans.repayment.descriptionWithContact', {name: props.loan.contact.name});
});

function makeBlankForm(): RepaymentFormModel {
  return {
    accountId: props.loan?.accountId,
    affectBalance: props.loan?.affectsBalance ?? true,
    amount: Number(props.loan?.outstanding ?? 0),
    description: '',
    repaidAt: todayIsoDate(),
  };
}

const getSnapshot = () => form.value;
const onResetOnOpen = () => {
  form.value = makeBlankForm();
};

async function handleSave(_event: FormSubmitEvent<Schema>) {
  if (!props.loan?.id || !props.loan.contact.id) return;

  loading.value = true;
  try {
    await loansStore.recordRepayment(props.loan.id, props.loan.contact.id, form.value);
    emit('recorded');
    isOpen.value = false;
  } catch (error) {
    adze.ns('loans').error('Record repayment failed', error);
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <FormModal v-model="isOpen"
             :description="description"
             :title="t('contacts.loans.repayment.title')"
             :schema="schema"
             :state="form"
             :loading="loading"
             :get-snapshot="getSnapshot"
             :on-reset-on-open="onResetOnOpen"
             :submit-label="t('contacts.loans.repayment.submit')"
             @submit="handleSave">
    <template #fields>
      <RepaymentForm v-if="loan" v-model="form" :max-amount="Number(loan.outstanding)" :currency="loan.currency"/>
    </template>
  </FormModal>
</template>
