<script lang="ts" setup>
import ThemeModeToggle from '~/components/_molecules/theme/theme-mode-toggle.vue';
import ThemeOption from '~/components/_molecules/theme/theme-option.vue';
import {swatchFor, useTheme} from '~/composables/use-theme';

const {t} = useI18n();
const {themes, current, mode, setTheme} = useTheme();
</script>

<template>
  <div>
    <h3 class="text-lg font-semibold">{{ t('profile.theme.title') }}</h3>
    <p class="text-sm text-muted mt-1">{{ t('profile.theme.description') }}</p>

    <div class="mt-4 space-y-4">
      <div>
        <div class="text-xs font-semibold text-muted uppercase tracking-wide mb-1.5">
          {{ t('profile.theme.appearance') }}
        </div>
        <ThemeModeToggle v-model="mode"/>
      </div>

      <div>
        <div class="text-xs font-semibold text-muted uppercase tracking-wide mb-1.5">
          {{ t('profile.theme.paletteTitle') }}
        </div>
        <div class="grid grid-cols-2 sm:grid-cols-3 gap-1">
          <ThemeOption v-for="theme in themes"
                       :key="theme.id"
                       :color="swatchFor(theme.primary)"
                       :label="theme.label"
                       :selected="current.id === theme.id"
                       @select="setTheme(theme.id)"/>
        </div>
      </div>
    </div>
  </div>
</template>
