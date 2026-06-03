<script lang="ts" setup>
import AppButton from "~/components/_atoms/ui/app-button.vue";

const {t} = useI18n();
</script>

<template>
  <AppButton color="neutral" v-bind="$attrs" variant="ghost">
    <slot>{{ t('common.actions.cancel') }}</slot>
  </AppButton>
</template>
