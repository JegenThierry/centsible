<script lang="ts" setup>
import ThemeOption from "~/components/_molecules/theme/theme-option.vue";
import ThemeModeToggle from "~/components/_molecules/theme/theme-mode-toggle.vue";
import {swatchFor, useTheme} from "~/composables/use-theme";

const {themes, current, mode, setTheme} = useTheme();
const open = ref(false);
</script>

<template>
  <UPopover v-model:open="open">
    <UButton aria-label="Change theme"
             color="neutral"
             icon="i-lucide-palette"
             variant="ghost"/>

    <template #content>
      <div class="p-3 w-72 flex flex-col gap-4">
        <div class="flex flex-col gap-1.5">
          <div class="text-xs font-semibold text-muted uppercase tracking-wide px-0.5">
            Appearance
          </div>
          <ThemeModeToggle v-model="mode"/>
        </div>

        <div class="flex flex-col gap-1.5">
          <div class="text-xs font-semibold text-muted uppercase tracking-wide px-0.5">
            Theme
          </div>
          <div class="grid grid-cols-2 gap-1">
            <ThemeOption v-for="theme in themes"
                         :key="theme.id"
                         :color="swatchFor(theme.primary)"
                         :label="theme.label"
                         :selected="current.id === theme.id"
                         @select="setTheme(theme.id)"/>
          </div>
        </div>
      </div>
    </template>
  </UPopover>
</template>
