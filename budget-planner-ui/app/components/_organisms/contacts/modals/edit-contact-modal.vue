<script lang="ts" setup>
import type {Contact, ContactForm as ContactFormModel} from "~/models/contact/contact";
import ContactForm from "~/components/_molecules/contacts/contact-form.vue";
import CancelButton from "~/components/_molecules/buttons/cancel-button.vue";
import {useContactsStore} from "~/stores/contactsStore";

const props = defineProps<{
  contact: Contact | undefined;
}>();

const isOpen = defineModel<boolean>('open', {required: true});

const contactsStore = useContactsStore();

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
  } catch {
    // toast handled by store
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <UModal v-model:open="isOpen"
          description="Update this contact's details."
          title="Edit Contact">
    <template #body>
      <ContactForm ref="formRef" v-model="form"/>
    </template>

    <template #footer>
      <div class="flex justify-end gap-2">
        <CancelButton @click="isOpen = false"/>
        <UButton :loading="loading" @click="handleSave">Save</UButton>
      </div>
    </template>
  </UModal>
</template>
