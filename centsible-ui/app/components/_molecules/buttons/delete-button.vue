<script lang="ts" setup>
defineProps<{
  loading?: boolean;
}>();

const {t} = useI18n();
</script>

<template>
  <UButton :loading="loading" color="error" v-bind="$attrs">
    <slot>{{ t('common.actions.delete') }}</slot>
  </UButton>
</template>
