<script lang="ts" setup>
import adze from 'adze'
import {z} from 'zod'
import type {FormSubmitEvent} from '@nuxt/ui'
import type {Contact, ContactForm as ContactFormModel} from "~/models/contact/contact";
import ContactForm from "~/components/_molecules/contacts/contact-form.vue";
import ModalFooterActions from "~/components/_molecules/modals/modal-footer-actions.vue";
import {useContactsStore} from "~/stores/contactsStore";
import {useModalDirtyGuard} from "~/composables/use-unsaved-changes-guard";
import {contactSchema} from "~/utils/form-schemas";

const props = defineProps<{
  /** Present → edit that contact; absent → create a new one. */
  contact?: Contact;
}>();

const isOpen = defineModel<boolean>('open', {required: true});

const contactsStore = useContactsStore();
const {t} = useI18n();

const form = ref<ContactFormModel>({firstName: '', lastName: ''});
const loading = ref(false);
const formId = useId();

const schema = contactSchema(t);
type Schema = z.output<typeof schema>

const isEdit = computed(() => !!props.contact);

function syncForm() {
  form.value = props.contact
    ? {firstName: props.contact.firstName, lastName: props.contact.lastName ?? ''}
    : {firstName: '', lastName: ''};
}

const {requestClose} = useModalDirtyGuard({
  isOpen,
  loading,
  getSnapshot: () => form.value,
  onResetOnOpen: syncForm,
});
watch(() => props.contact, () => {
  if (isOpen.value) syncForm();
});

async function handleSave(_event: FormSubmitEvent<Schema>) {
  loading.value = true;
  try {
    if (props.contact) {
      if (!props.contact.id) return;
      await contactsStore.updateContact(props.contact.id, form.value);
    } else {
      await contactsStore.createContact(form.value);
    }
    isOpen.value = false;
  } catch (error) {
    adze.ns('contacts').error(`${isEdit.value ? 'Update' : 'Create'} contact failed`, error);
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <UModal :open="isOpen"
          :description="t(isEdit ? 'contacts.edit.description' : 'contacts.create.description')"
          :title="t(isEdit ? 'contacts.edit.title' : 'contacts.create.title')"
          @update:open="requestClose">
    <template #body>
      <UForm :id="formId" :schema="schema" :state="form" @submit="handleSave">
        <ContactForm v-model="form"/>
      </UForm>
    </template>

    <template #footer>
      <ModalFooterActions :form="formId"
                          :loading="loading"
                          :submit-label="t(isEdit ? 'contacts.edit.submit' : 'contacts.create.submit')"
                          @cancel="requestClose(false)"/>
    </template>
  </UModal>
</template>
