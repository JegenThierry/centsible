<script lang="ts" setup>
import ContactDetail from "~/components/_organisms/contacts/contact-detail.vue";

definePageMeta({
  middleware: ['auth-guard']
})

const route = useRoute();
const contactId = computed(() => String(route.params.id ?? ''));

const {t} = useI18n();

useHead({
  title: t('contacts.page.title'),
});
</script>

<template>
  <ContactDetail :contact-id="contactId"/>
</template>
