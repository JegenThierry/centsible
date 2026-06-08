<script lang="ts" setup>
import {computeOwedFromLent, hasInterestRate, type LoanForm} from "~/models/loan/loan";
import BaseInput from "~/components/_atoms/inputs/base-input.vue";
import DateInput from "~/components/_atoms/inputs/date-input.vue";

defineProps<{
  currency?: string;
  disabled?: boolean;
}>();

const form = defineModel<LoanForm>({required: true});

const {t} = useI18n();

const hasInterest = computed(() => hasInterestRate(form.value.interestRate));

watch([() => form.value.interestRate, () => form.value.lentAmount], () => {
  if (!hasInterest.value) return;
  const owed = computeOwedFromLent(form.value.lentAmount, form.value.interestRate);
  if (Number(form.value.owedAmount) !== owed) form.value = {...form.value, owedAmount: owed};
});
</script>

<template>
  <div class="space-y-4">
    <BaseInput name="lentAmount"
               v-model="form.lentAmount"
               :max="9999999.99"
               :min="0.01"
               :disabled="disabled"
               :label="t('contacts.loans.form.lentLabel')"
               :placeholder="t('contacts.loans.form.lentPlaceholder')"
               :trailing-text="currency"
               required
               type="number"/>

    <BaseInput name="interestRate"
               v-model="form.interestRate"
               :max="999.99"
               :min="0"
               :disabled="disabled"
               :label="t('contacts.loans.form.interestRateLabel')"
               :description="t('contacts.loans.form.interestRateDescription')"
               :placeholder="t('contacts.loans.form.interestRatePlaceholder')"
               trailing-text="%"
               type="number"/>

    <BaseInput name="owedAmount"
               v-model="form.owedAmount"
               :max="9999999.99"
               :min="0"
               :description="hasInterest ? t('contacts.loans.form.owedComputedDescription') : t('contacts.loans.form.owedDescription')"
               :disabled="disabled || hasInterest"
               :label="t('contacts.loans.form.owedLabel')"
               :placeholder="t('contacts.loans.form.owedPlaceholder')"
               :trailing-text="currency"
               required
               type="number"/>

    <BaseInput name="description"
               v-model="form.description"
               :max-length="255"
               :disabled="disabled"
               :label="t('contacts.loans.form.descriptionLabel')"
               :placeholder="t('contacts.loans.form.descriptionPlaceholder')"
               required
               type="text"/>

    <DateInput name="transactionDate"
               v-model="form.transactionDate"
               :disabled="disabled"
               :label="t('contacts.loans.form.dateLabel')"
               required/>

    <DateInput name="dueDate"
               v-model="form.dueDate"
               :disabled="disabled"
               :label="t('contacts.loans.form.dueDateLabel')"/>

    <BaseInput name="notes"
               v-model="form.notes"
               :max-length="500"
               :disabled="disabled"
               :label="t('contacts.loans.form.notesLabel')"
               :placeholder="t('contacts.loans.form.notesPlaceholder')"
               type="text"/>
  </div>
</template>
