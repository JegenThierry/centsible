<script lang="ts" setup>
import adze from 'adze'
import {z} from 'zod'
import type {FormSubmitEvent} from '@nuxt/ui'
import type {Contact, ContactForm as ContactFormModel} from "~/models/contact/contact";
import ContactForm from "~/components/_molecules/contacts/contact-form.vue";
import ModalFooterActions from "~/components/_molecules/modals/modal-footer-actions.vue";
import {useContactsStore} from "~/stores/contactsStore";
import {contactSchema} from "~/utils/form-schemas";

const props = defineProps<{
  contact: Contact | undefined;
}>();

const isOpen = defineModel<boolean>('open', {required: true});

const contactsStore = useContactsStore();
const {t} = useI18n();

const form = ref<ContactFormModel>({firstName: '', lastName: ''});
const loading = ref(false);

const schema = contactSchema(t);
type Schema = z.output<typeof schema>

watch(() => props.contact, (c) => {
  if (c) {
    form.value = {firstName: c.firstName, lastName: c.lastName ?? ''};
  }
}, {immediate: true});

async function handleSave(_event: FormSubmitEvent<Schema>) {
  if (!props.contact?.id) return;

  loading.value = true;
  try {
    await contactsStore.updateContact(props.contact.id, form.value);
    isOpen.value = false;
  } catch (error) {
    adze.ns('contacts').error('Update contact failed', error);
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <UModal v-model:open="isOpen"
          :description="t('contacts.edit.description')"
          :title="t('contacts.edit.title')">
    <template #body>
      <UForm id="edit-contact-form" :schema="schema" :state="form" @submit="handleSave">
        <ContactForm v-model="form"/>
      </UForm>
    </template>

    <template #footer>
      <ModalFooterActions form="edit-contact-form"
                          :loading="loading"
                          :submit-label="t('contacts.edit.submit')"
                          @cancel="isOpen = false"/>
    </template>
  </UModal>
</template>
