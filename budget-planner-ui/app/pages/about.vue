<script lang="ts" setup>
import About from "~/components/_organisms/about/about.vue";

const {t} = useI18n();
useHead({
  title: t('landing.aboutPageTitle'),
});
</script>

<template>
  <About/>
</template>
