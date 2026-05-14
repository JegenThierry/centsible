<script lang="ts" setup>
import type {ContactForm as ContactFormModel} from "~/models/contact/contact";
import ContactForm from "~/components/_molecules/contacts/contact-form.vue";
import CancelButton from "~/components/_molecules/buttons/cancel-button.vue";
import {useContactsStore} from "~/stores/contactsStore";

const isOpen = defineModel<boolean>('open', {required: true});

const contactsStore = useContactsStore();

const form = ref<ContactFormModel>({firstName: '', lastName: ''});
const formRef = ref<InstanceType<typeof ContactForm>>();
const loading = ref(false);

function resetForm() {
  form.value = {firstName: '', lastName: ''};
}

watch(isOpen, (open) => {
  if (open) resetForm();
});

async function handleSave() {
  if (!formRef.value?.validate()) return;

  loading.value = true;
  try {
    await contactsStore.createContact(form.value);
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
          description="Track someone you've lent money to."
          title="Add Contact">
    <template #body>
      <ContactForm ref="formRef" v-model="form"/>
    </template>

    <template #footer>
      <div class="flex justify-end gap-2">
        <CancelButton @click="isOpen = false"/>
        <UButton :loading="loading" @click="handleSave">Add</UButton>
      </div>
    </template>
  </UModal>
</template>
