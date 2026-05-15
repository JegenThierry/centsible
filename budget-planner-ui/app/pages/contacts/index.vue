<script lang="ts" setup>
import Contacts from "~/components/_organisms/contacts/contacts.vue";

definePageMeta({
  middleware: ['auth-guard']
})

useHead({
  title: 'Contacts',
});
</script>

<template>
  <Contacts/>
</template>
