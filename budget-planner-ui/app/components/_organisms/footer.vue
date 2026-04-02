<script setup lang="ts">
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
      <p class="text-muted text-sm">Copyright © {{ new Date().getFullYear() }}</p>
    </template>

    <UNavigationMenu :items="items" variant="link" />

    <template #right>
      <UButton
          icon="i-simple-icons-instagram"
          color="neutral"
          variant="ghost"
          to="https://www.instagram.com/thierryjegen/"
          target="_blank"
          aria-label="Instagram"
      />
      <UButton
          icon="i-simple-icons-x"
          color="neutral"
          variant="ghost"
          to="https://x.com/ThierryJegen1"
          target="_blank"
          aria-label="X"
      />
      <UButton
          icon="i-simple-icons-codeberg"
          color="neutral"
          variant="ghost"
          to="https://codeberg.org/thierryjegen"
          target="_blank"
          aria-label="Codeberg"
      />
    </template>
  </UFooter>
</template>
