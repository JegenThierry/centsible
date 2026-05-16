<script lang="ts" setup>
const {t} = useI18n();
</script>

<template>
  <UButton color="neutral" v-bind="$attrs" variant="ghost">
    <slot>{{ t('common.actions.cancel') }}</slot>
  </UButton>
</template>
