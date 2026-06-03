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

const lentInput = ref<InstanceType<typeof BaseInput>>();
const owedInput = ref<InstanceType<typeof BaseInput>>();
const descriptionInput = ref<InstanceType<typeof BaseInput>>();
const dateInput = ref<InstanceType<typeof DateInput>>();

defineExpose({
  validate: () => useValidator().validateInputs([lentInput, owedInput, descriptionInput, dateInput]),
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

    <BaseInput ref="owedInput"
               v-model="form.owedAmount"
               :max="9999999.99"
               :min="0"
               :description="t('contacts.loans.form.owedDescription')"
               :disabled="disabled"
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
  </div>
</template>
