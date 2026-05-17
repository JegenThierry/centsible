<script lang="ts" setup>
import type {Contact} from "~/models/contact/contact";
import CancelButton from "~/components/_molecules/buttons/cancel-button.vue";
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
  } catch {
    // toast handled by store
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
      <div class="flex justify-end gap-2">
        <CancelButton @click="isOpen = false"/>
        <UButton :loading="loading" color="error" @click="handleDelete">{{ t('contacts.delete.submit') }}</UButton>
      </div>
    </template>
  </UModal>
</template>
