<script lang="ts" setup>
import AppButton from "~/components/_atoms/ui/app-button.vue";

const emit = defineEmits<{
  (e: 'refresh'): void;
}>();

const {t} = useI18n();

function onRefresh() {
  emit('refresh');
}
</script>

<template>
  <AppButton color="neutral" icon="i-lucide-refresh-cw" variant="subtle" @click="onRefresh">
    {{ t('common.actions.refresh') }}
  </AppButton>
</template>
