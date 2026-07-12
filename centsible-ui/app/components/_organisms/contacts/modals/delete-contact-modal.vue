<script lang="ts" setup>
import ConfirmationModal from "~/components/_organisms/modals/confirmation-modal.vue";
import type {Contact} from "~/models/contact/contact";
import {useContactsStore} from "~/stores/contactsStore";

const props = defineProps<{
  contact: Contact | undefined;
}>();

const isOpen = defineModel<boolean>('open', {required: true});

const emit = defineEmits<{
  (e: 'deleted'): void;
}>();

const contactsStore = useContactsStore();
const {t} = useI18n();

const description = computed(() => {
  if (props.contact?.name) {
    return t('contacts.delete.descriptionWithName', {name: props.contact.name});
  }
  return t('contacts.delete.descriptionGeneric');
});

async function deleteContact() {
  if (!props.contact?.id) return;
  await contactsStore.deleteContact(props.contact.id);
  emit('deleted');
}
</script>

<template>
  <ConfirmationModal v-model:open="isOpen"
                     :title="t('contacts.delete.title')"
                     :body="description"
                     :confirm-label="t('contacts.delete.submit')"
                     :manage-toasts="false"
                     :delete-callback="deleteContact"/>
</template>
