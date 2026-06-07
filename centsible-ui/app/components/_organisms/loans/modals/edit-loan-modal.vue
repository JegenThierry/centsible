<script lang="ts" setup>
import adze from 'adze'
import {type Loan, type LoanUpdateForm, cleanOptionalNumber} from "~/models/loan/loan";
import BaseInput from "~/components/_atoms/inputs/base-input.vue";
import DateInput from "~/components/_atoms/inputs/date-input.vue";
import ModalFooterActions from "~/components/_molecules/modals/modal-footer-actions.vue";
import {useLoansStore} from "~/stores/loansStore";
import {useValidator} from "~/composables/use-validator";

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

const owedInput = ref<InstanceType<typeof BaseInput>>();
const interestInput = ref<InstanceType<typeof BaseInput>>();
const descriptionInput = ref<InstanceType<typeof BaseInput>>();

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

watch(isOpen, (open) => {
  if (open) form.value = makeForm();
});

// When interest is set, owed is derived from the (immutable) lent amount and read-only.
const hasInterest = computed(() => {
  const r = form.value.interestRate;
  return r !== undefined && r !== null && String(r) !== '' && Number(r) > 0;
});

watch(() => form.value.interestRate, () => {
  if (!hasInterest.value || !props.loan) return;
  const owed = Math.round(Number(props.loan.lentAmount) * (1 + Number(form.value.interestRate) / 100) * 100) / 100;
  if (Number(form.value.owedAmount) !== owed) form.value = {...form.value, owedAmount: owed};
});

async function handleSave() {
  if (!props.loan) return;
  if (!useValidator().validateInputs([owedInput, interestInput, descriptionInput])) return;

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
  <UModal v-model:open="isOpen"
          :description="t('contacts.loans.edit.description')"
          :title="t('contacts.loans.edit.title')">
    <template #body>
      <div class="space-y-4">
        <BaseInput v-model="form.description"
                   ref="descriptionInput"
                   :max-length="255"
                   :label="t('contacts.loans.form.descriptionLabel')"
                   :placeholder="t('contacts.loans.form.descriptionPlaceholder')"
                   required
                   type="text"/>

        <BaseInput ref="interestInput"
                   v-model="form.interestRate"
                   :max="999.99"
                   :min="0"
                   :label="t('contacts.loans.form.interestRateLabel')"
                   :description="t('contacts.loans.form.interestRateDescription')"
                   :placeholder="t('contacts.loans.form.interestRatePlaceholder')"
                   trailing-text="%"
                   type="number"/>

        <BaseInput ref="owedInput"
                   v-model="form.owedAmount"
                   :max="9999999.99"
                   :min="0"
                   :description="hasInterest ? t('contacts.loans.form.owedComputedDescription') : t('contacts.loans.form.owedDescription')"
                   :disabled="hasInterest"
                   :label="t('contacts.loans.form.owedLabel')"
                   :placeholder="t('contacts.loans.form.owedPlaceholder')"
                   :trailing-text="loan?.currency"
                   required
                   type="number"/>

        <DateInput v-model="form.dueDate"
                   :label="t('contacts.loans.form.dueDateLabel')"/>

        <BaseInput v-model="form.notes"
                   :max-length="500"
                   :label="t('contacts.loans.form.notesLabel')"
                   :placeholder="t('contacts.loans.form.notesPlaceholder')"
                   type="text"/>
      </div>
    </template>

    <template #footer>
      <ModalFooterActions :loading="loading"
                          :submit-label="t('contacts.loans.edit.submit')"
                          @cancel="isOpen = false"
                          @submit="handleSave"/>
    </template>
  </UModal>
</template>
