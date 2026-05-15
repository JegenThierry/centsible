<script lang="ts" setup>
import Accounts from "~/components/_organisms/accounts/accounts.vue";

definePageMeta({
  middleware: ['auth-guard']
})

useHead({
  title: 'Accounts',
});
</script>

<template>
  <Accounts/>
</template>
