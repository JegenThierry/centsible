<script lang="ts" setup>
import CancelButton from "~/components/_molecules/buttons/cancel-button.vue";

defineProps<{
  submitLabel: string;
  loading?: boolean;
  disabled?: boolean;
  cancelDisabled?: boolean;
  submitColor?: 'primary' | 'error' | 'success' | 'warning' | 'info' | 'neutral';
  submitIcon?: string;
  form?: string;
}>();

const emit = defineEmits<{
  (e: 'cancel'): void;
  (e: 'submit'): void;
}>();
</script>

<template>
  <div class="flex justify-end gap-2">
    <CancelButton :disabled="cancelDisabled || loading" @click="emit('cancel')"/>
    <UButton :color="submitColor ?? 'primary'"
             :disabled="disabled"
             :form="form"
             :icon="submitIcon"
             :loading="loading"
             :type="form ? 'submit' : 'button'"
             @click="form ? undefined : emit('submit')">
      {{ submitLabel }}
    </UButton>
  </div>
</template>
