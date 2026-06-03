<script lang="ts" setup>
import type {Contact} from "~/models/contact/contact";
import ContactAvatarUploader from "~/components/_molecules/contacts/contact-avatar-uploader.vue";
import ExportButton from "~/components/_molecules/exports/export-button.vue";
import AppButton from "~/components/_atoms/ui/app-button.vue";

const props = defineProps<{
  contact: Contact;
}>();

defineEmits<{
  edit: [];
  delete: [];
}>();

const {t} = useI18n();
</script>

<template>
  <UCard class="mb-6">
    <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
      <div class="flex items-center gap-4">
        <ContactAvatarUploader :contact-id="contact.id"
                               :name="contact.name"
                               :picture="contact.picture"/>
        <div>
          <h2 class="text-2xl font-bold">{{ contact.name }}</h2>
          <p class="text-sm text-neutral-500">
            {{ t('contacts.detail.loanCount', {count: contact.openLoanCount}, contact.openLoanCount) }}
          </p>
        </div>
      </div>
      <div class="flex gap-2">
        <ExportButton
          :default-title="t('contacts.detail.exportDefaultTitle', {name: contact.name})"
          :params-builder="() => ({ kind: 'LENDINGS_PER_CONTACT', contactId: contact.id })"
          :label="t('contacts.detail.exportLabel')"
          type="LENDINGS_PER_CONTACT"
        />
        <AppButton color="neutral"
                   icon="i-lucide-pencil"
                   variant="outline"
                   @click="$emit('edit')">
          {{ t('contacts.detail.edit') }}
        </AppButton>
        <AppButton color="error"
                   icon="i-lucide-trash"
                   variant="outline"
                   @click="$emit('delete')">
          {{ t('contacts.detail.delete') }}
        </AppButton>
      </div>
    </div>
  </UCard>
</template>
