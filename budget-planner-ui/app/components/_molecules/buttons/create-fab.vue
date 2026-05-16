<script lang="ts" setup>
const emit = defineEmits<{
  (e: 'create'): void;
}>();

const {t} = useI18n();

function onCreate() {
  emit('create');
}
</script>

<template>
  <UButton :aria-label="t('common.actions.create')"
           class="fixed bottom-12 right-12 rounded-full shadow-lg z-50"
           color="primary"
           icon="i-lucide-plus"
           size="xl"
           @click="onCreate"/>
</template>
