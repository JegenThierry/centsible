<script lang="ts" setup>
import type {Contact, ContactForm as ContactFormModel} from "~/models/contact/contact";
import ContactForm from "~/components/_molecules/contacts/contact-form.vue";
import ModalFooterActions from "~/components/_molecules/modals/modal-footer-actions.vue";
import {useContactsStore} from "~/stores/contactsStore";

const props = defineProps<{
  contact: Contact | undefined;
}>();

const isOpen = defineModel<boolean>('open', {required: true});

const contactsStore = useContactsStore();
const {t} = useI18n();

const form = ref<ContactFormModel>({firstName: '', lastName: ''});
const formRef = ref<InstanceType<typeof ContactForm>>();
const loading = ref(false);

watch(() => props.contact, (c) => {
  if (c) {
    form.value = {firstName: c.firstName, lastName: c.lastName ?? ''};
  }
}, {immediate: true});

async function handleSave() {
  if (!props.contact?.id) return;
  if (!formRef.value?.validate()) return;

  loading.value = true;
  try {
    await contactsStore.updateContact(props.contact.id, form.value);
    isOpen.value = false;
  } catch (error) {
    console.error('Update contact failed', error);
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
      <ContactForm ref="formRef" v-model="form"/>
    </template>

    <template #footer>
      <ModalFooterActions :loading="loading"
                          :submit-label="t('contacts.edit.submit')"
                          @cancel="isOpen = false"
                          @submit="handleSave"/>
    </template>
  </UModal>
</template>
