<script lang="ts" setup>
import type {LoanForm} from "~/models/loan/loan";
import type {Contact} from "~/models/contact/contact";
import BaseInput from "~/components/_atoms/inputs/base-input.vue";
import ContactSelect from "~/components/_atoms/inputs/contact-select.vue";
import AppRadioGroup from "~/components/_atoms/ui/app-radio-group.vue";
import {useValidator} from "~/composables/use-validator";

defineProps<{
  contacts: Contact[];
  lockContact?: boolean;
  disabled?: boolean;
}>();

const form = defineModel<LoanForm>({required: true});
const mode = defineModel<'existing' | 'new'>('mode', {required: true});
const selectedContact = defineModel<Contact | undefined>('selectedContact');

const {t} = useI18n();

const contactSelect = ref<InstanceType<typeof ContactSelect>>();
const firstNameInput = ref<InstanceType<typeof BaseInput>>();
const lastNameInput = ref<InstanceType<typeof BaseInput>>();

const modeOptions = computed(() => [
  {label: t('contacts.loans.form.modeExisting'), value: 'existing'},
  {label: t('contacts.loans.form.modeNew'), value: 'new'},
]);

defineExpose({
  validate: () => {
    const inputs = mode.value === 'existing' ? [contactSelect] : [firstNameInput];
    return useValidator().validateInputs(inputs);
  },
});
</script>

<template>
  <div class="space-y-4">
    <AppRadioGroup v-if="!lockContact"
                   v-model="mode"
                   :disabled="disabled"
                   :items="modeOptions"
                   :legend="t('contacts.loans.form.modeLegend')"
                   orientation="horizontal"/>

    <ContactSelect v-if="mode === 'existing' && !lockContact"
                   ref="contactSelect"
                   v-model="selectedContact"
                   :disabled="disabled"
                   :options="contacts"
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
  </div>
</template>
