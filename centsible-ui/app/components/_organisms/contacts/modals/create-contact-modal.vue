<script lang="ts" setup>
import type {ContactForm as ContactFormModel} from "~/models/contact/contact";
import ContactForm from "~/components/_molecules/contacts/contact-form.vue";
import CancelButton from "~/components/_molecules/buttons/cancel-button.vue";
import {useContactsStore} from "~/stores/contactsStore";

const isOpen = defineModel<boolean>('open', {required: true});

const contactsStore = useContactsStore();
const {t} = useI18n();

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
  } catch (error) {
    console.error('Create contact failed', error);
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
      <ContactForm ref="formRef" v-model="form"/>
    </template>

    <template #footer>
      <div class="flex justify-end gap-2">
        <CancelButton @click="isOpen = false"/>
        <UButton :loading="loading" @click="handleSave">{{ t('contacts.create.submit') }}</UButton>
      </div>
    </template>
  </UModal>
</template>
