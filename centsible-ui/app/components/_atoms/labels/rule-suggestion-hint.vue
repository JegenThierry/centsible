<script lang="ts" setup>
defineProps<{
  rule: string | null | undefined;
}>();

const {t} = useI18n();
</script>

<template>
  <p v-if="rule" class="flex items-center gap-1 px-1 text-xs text-muted">
    <UIcon name="i-lucide-sparkles" class="size-3.5 shrink-0 text-primary"/>
    <span>{{ t('transactions.form.ruleHint', {rule}) }}</span>
  </p>
</template>
