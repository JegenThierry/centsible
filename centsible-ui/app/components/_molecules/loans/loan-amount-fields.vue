<script lang="ts" setup>
import type {LoanForm} from "~/models/loan/loan";
import BaseInput from "~/components/_atoms/inputs/base-input.vue";
import DateInput from "~/components/_atoms/inputs/date-input.vue";
import {useValidator} from "~/composables/use-validator";

defineProps<{
  currency?: string;
  disabled?: boolean;
}>();

const form = defineModel<LoanForm>({required: true});

const {t} = useI18n();

// When an interest rate is set, owed is derived (owed = lent * (1 + rate/100)) and read-only.
const hasInterest = computed(() => {
  const r = form.value.interestRate;
  return r !== undefined && r !== null && String(r) !== '' && Number(r) > 0;
});

watch([() => form.value.interestRate, () => form.value.lentAmount], () => {
  if (!hasInterest.value) return;
  const owed = Math.round(Number(form.value.lentAmount) * (1 + Number(form.value.interestRate) / 100) * 100) / 100;
  if (Number(form.value.owedAmount) !== owed) form.value = {...form.value, owedAmount: owed};
});

const lentInput = ref<InstanceType<typeof BaseInput>>();
const owedInput = ref<InstanceType<typeof BaseInput>>();
const interestInput = ref<InstanceType<typeof BaseInput>>();
const descriptionInput = ref<InstanceType<typeof BaseInput>>();
const dateInput = ref<InstanceType<typeof DateInput>>();

defineExpose({
  validate: () => useValidator().validateInputs([lentInput, owedInput, interestInput, descriptionInput, dateInput]),
});
</script>

<template>
  <div class="space-y-4">
    <BaseInput ref="lentInput"
               v-model="form.lentAmount"
               :max="9999999.99"
               :min="0.01"
               :disabled="disabled"
               :label="t('contacts.loans.form.lentLabel')"
               :placeholder="t('contacts.loans.form.lentPlaceholder')"
               :trailing-text="currency"
               required
               type="number"/>

    <BaseInput ref="interestInput"
               v-model="form.interestRate"
               :max="999.99"
               :min="0"
               :disabled="disabled"
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
               :disabled="disabled || hasInterest"
               :label="t('contacts.loans.form.owedLabel')"
               :placeholder="t('contacts.loans.form.owedPlaceholder')"
               :trailing-text="currency"
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

    <DateInput v-model="form.dueDate"
               :disabled="disabled"
               :label="t('contacts.loans.form.dueDateLabel')"/>

    <BaseInput v-model="form.notes"
               :max-length="500"
               :disabled="disabled"
               :label="t('contacts.loans.form.notesLabel')"
               :placeholder="t('contacts.loans.form.notesPlaceholder')"
               type="text"/>
  </div>
</template>
