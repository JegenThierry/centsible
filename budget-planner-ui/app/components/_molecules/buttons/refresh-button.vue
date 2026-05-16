<script lang="ts" setup>
const emit = defineEmits<{
  (e: 'refresh'): void;
}>();

const {t} = useI18n();

function onRefresh() {
  emit('refresh');
}
</script>

<template>
  <UButton color="neutral" icon="i-lucide-refresh-cw" variant="subtle" @click="onRefresh">
    {{ t('common.actions.refresh') }}
  </UButton>
</template>
