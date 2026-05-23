<script lang="ts" setup>
import type {Contact} from "~/models/contact/contact";
import ModalFooterActions from "~/components/_molecules/modals/modal-footer-actions.vue";
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
const loading = ref(false);

const description = computed(() => {
  if (props.contact?.name) {
    return t('contacts.delete.descriptionWithName', {name: props.contact.name});
  }
  return t('contacts.delete.descriptionGeneric');
});

async function handleDelete() {
  if (!props.contact?.id) return;

  loading.value = true;
  try {
    await contactsStore.deleteContact(props.contact.id);
    isOpen.value = false;
    emit('deleted');
  } catch (error) {
    console.error('Delete contact failed', error);
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <UModal v-model:open="isOpen"
          :description="description"
          :title="t('contacts.delete.title')">
    <template #footer>
      <ModalFooterActions :loading="loading"
                          :submit-label="t('contacts.delete.submit')"
                          submit-color="error"
                          @cancel="isOpen = false"
                          @submit="handleDelete"/>
    </template>
  </UModal>
</template>
