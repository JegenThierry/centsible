<script lang="ts" setup>
import OauthReturnHandler from "~/components/_organisms/integrations/oauth-return-handler.vue";

definePageMeta({
  middleware: ['auth-guard'],
});

const {t} = useI18n();

useHead({
  title: t('integrations.oauthReturn.title'),
});
</script>

<template>
  <OauthReturnHandler/>
</template>
