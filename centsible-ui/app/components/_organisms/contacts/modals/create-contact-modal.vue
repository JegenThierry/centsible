<script lang="ts" setup>
import adze from 'adze'
import {z} from 'zod'
import type {FormSubmitEvent} from '@nuxt/ui'
import type {ContactForm as ContactFormModel} from "~/models/contact/contact";
import ContactForm from "~/components/_molecules/contacts/contact-form.vue";
import ModalFooterActions from "~/components/_molecules/modals/modal-footer-actions.vue";
import {useContactsStore} from "~/stores/contactsStore";

const isOpen = defineModel<boolean>('open', {required: true});

const contactsStore = useContactsStore();
const {t} = useI18n();

const form = ref<ContactFormModel>({firstName: '', lastName: ''});
const loading = ref(false);

const firstNameLabel = t('contacts.form.firstNameLabel');
const lastNameLabel = t('contacts.form.lastNameLabel');

const schema = z.object({
  firstName: z.string().trim()
    .min(1, t('common.validation.required', {field: firstNameLabel}))
    .max(100, t('common.validation.maxLength', {field: firstNameLabel, max: 100})),
  lastName: z.string().trim()
    .max(100, t('common.validation.maxLength', {field: lastNameLabel, max: 100}))
    .optional(),
})
type Schema = z.output<typeof schema>

function resetForm() {
  form.value = {firstName: '', lastName: ''};
}

watch(isOpen, (open) => {
  if (open) resetForm();
});

async function handleSave(_event: FormSubmitEvent<Schema>) {
  loading.value = true;
  try {
    await contactsStore.createContact(form.value);
    isOpen.value = false;
  } catch (error) {
    adze.ns('contacts').error('Create contact failed', error);
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <UModal v-model:open="isOpen"
          :description="t('contacts.create.description')"
          :title="t('contacts.create.title')">
    <template #body>
      <UForm id="create-contact-form" :schema="schema" :state="form" @submit="handleSave">
        <ContactForm v-model="form"/>
      </UForm>
    </template>

    <template #footer>
      <ModalFooterActions form="create-contact-form"
                          :loading="loading"
                          :submit-label="t('contacts.create.submit')"
                          @cancel="isOpen = false"/>
    </template>
  </UModal>
</template>
