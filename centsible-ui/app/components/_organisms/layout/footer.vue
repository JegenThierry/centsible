<script lang="ts" setup>
import type {NavigationMenuItem} from "@nuxt/ui/components/NavigationMenu.vue";
import AppButton from "~/components/_atoms/ui/app-button.vue";
import {REPOSITORY_URL} from "~/models/constants";

const {t} = useI18n();

const items = computed<NavigationMenuItem[]>(() => [
  {
    label: t('nav.footer.about'),
    to: '/about',
    target: '_self'
  },
  {
    label: t('nav.footer.activity'),
    to: `${REPOSITORY_URL}/activity`,
    target: '_blank'
  }
]);
</script>

<template>
  <UFooter>
    <template #left>
      <p class="text-muted text-sm">
        {{ t('nav.footer.copyright', {year: new Date().getFullYear()}) }}
      </p>
    </template>

    <UNavigationMenu :items="items"
                     variant="link"/>

    <template #right>
      <AppButton :aria-label="t('nav.footer.donate')"
               color="neutral"
               icon="i-simple-icons-kofi"
               target="_blank"
               to="https://ko-fi.com/thierryjegen"
               variant="ghost"/>

      <AppButton :aria-label="t('nav.footer.instagram')"
               color="neutral"
               icon="i-simple-icons-instagram"
               target="_blank"
               to="https://www.instagram.com/thierryjegen/"
               variant="ghost"/>

      <AppButton :aria-label="t('nav.footer.twitter')"
               color="neutral"
               icon="i-simple-icons-x"
               target="_blank"
               to="https://x.com/ThierryJegen1"
               variant="ghost"/>

      <AppButton :aria-label="t('nav.footer.codeberg')"
               color="neutral"
               icon="i-simple-icons-github"
               target="_blank"
               to="https://github.com/JegenThierry"
               variant="ghost"/>
    </template>
  </UFooter>
</template>
