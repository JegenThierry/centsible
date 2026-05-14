<script lang="ts" setup>
import Contacts from "~/components/_organisms/contacts/contacts.vue";

definePageMeta({
  middleware: ['auth-guard']
})

useHead({
  title: 'Contacts | Budget Planner',
});
</script>

<template>
  <Contacts/>
</template>
