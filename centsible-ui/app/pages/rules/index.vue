<script lang="ts" setup>
import RulesManager from "~/components/_organisms/rules/rules-manager.vue";

definePageMeta({
  middleware: ['auth-guard']
})

const {t} = useI18n();

useHead({
  title: t('rules.page.title'),
});
</script>

<template>
  <RulesManager/>
</template>
