<script lang="ts" setup>
import {useContactsStore} from "~/stores/contactsStore";
import ContactAvatar from "~/components/_atoms/contacts/contact-avatar.vue";

const props = defineProps<{
  contactId: string;
  name: string;
  picture?: string;
}>();

const contactsStore = useContactsStore();

const fileInput = ref<HTMLInputElement | null>(null);

function onEditAvatar() {
  fileInput.value?.click();
}

async function onFileChange(event: Event) {
  const target = event.target as HTMLInputElement;
  const file = target.files?.[0];
  if (!file) return;

  try {
    await contactsStore.updateContactPicture(props.contactId, file);
  } finally {
    if (fileInput.value) fileInput.value.value = '';
  }
}
</script>

<template>
  <ContactAvatar :alt="name"
                 :src="picture"
                 editable
                 size="3xl"
                 @edit="onEditAvatar"/>
  <input ref="fileInput"
         accept="image/*"
         class="hidden"
         type="file"
         @change="onFileChange"/>
</template>
