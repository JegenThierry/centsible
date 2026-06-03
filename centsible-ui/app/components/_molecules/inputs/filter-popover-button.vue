<script lang="ts" setup>
import {ref} from 'vue';
import AppButton from "~/components/_atoms/ui/app-button.vue";

defineProps<{
  label: string;
  icon?: string;
  count?: number;
  active?: boolean;
}>();

const open = ref(false);
</script>

<template>
  <UPopover v-model:open="open">
    <AppButton :color="active ? 'primary' : 'neutral'"
             :icon="icon"
             :variant="active ? 'subtle' : 'outline'"
             size="sm"
             trailing-icon="i-lucide-chevron-down">
      <span>{{ label }}</span>
      <span v-if="count && count > 0"
            class="ml-1 inline-flex items-center justify-center min-w-5 h-5 px-1.5 rounded-full bg-primary-500 text-white text-xs font-semibold">
        {{ count }}
      </span>
    </AppButton>

    <template #content>
      <div class="p-3">
        <slot :close="() => (open = false)"/>
      </div>
    </template>
  </UPopover>
</template>
