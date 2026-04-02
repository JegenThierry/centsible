<script lang="ts" setup>
import type {NavigationMenuItem} from "@nuxt/ui/components/NavigationMenu.vue";

const items: NavigationMenuItem[] = [
  {
    label: 'Activity',
    to: 'https://codeberg.org/thierryjegen/budget-planner/activity',
    target: '_blank'
  }
]
</script>

<template>
  <UFooter>
    <template #left>
      <p class="text-muted text-sm">
        Copyright © {{ new Date().getFullYear() }}
      </p>
    </template>

    <UNavigationMenu :items="items"
                     variant="link"/>

    <template #right>
      <UButton aria-label="Instagram"
               color="neutral"
               icon="i-simple-icons-instagram"
               target="_blank"
               to="https://www.instagram.com/thierryjegen/"
               variant="ghost"/>

      <UButton aria-label="X"
               color="neutral"
               icon="i-simple-icons-x"
               target="_blank"
               to="https://x.com/ThierryJegen1"
               variant="ghost"/>

      <UButton aria-label="Codeberg"
               color="neutral"
               icon="i-simple-icons-codeberg"
               target="_blank"
               to="https://codeberg.org/thierryjegen"
               variant="ghost"/>
    </template>
  </UFooter>
</template>
