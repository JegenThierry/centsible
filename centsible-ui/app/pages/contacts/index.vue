<script lang="ts" setup>
import Contacts from "~/components/_organisms/contacts/contacts.vue";

definePageMeta({
  middleware: ['auth-guard']
})

const {t} = useI18n();

useHead({
  title: t('contacts.page.title'),
});
</script>

<template>
  <Contacts/>
</template>
