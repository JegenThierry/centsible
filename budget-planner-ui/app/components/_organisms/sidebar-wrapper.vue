<template>
  <UDashboardSidebar
      v-model:open="isOpen"
      class="fixed sidebar-wrapper right-0"
      side="right"
      collapsible
  >
    <template #header>
      <div class="flex items-center justify-between w-full">
        <div class="flex-1 min-w-0">
          <slot name="header">
            <h3 class="text-base font-semibold text-gray-900 dark:text-white truncate">
              Sidebar
            </h3>
          </slot>
        </div>

        <UButton
            icon="i-heroicons-x-mark-20-solid"
            variant="ghost"
            aria-label="Close sidebar"
            :padded="false"
            class="ml-4"
            @click="isOpen = false"
        />
      </div>
    </template>

    <div class="flex-1 overflow-y-auto p-4 custom-scrollbar">
      <slot />
    </div>

    <template v-if="$slots.actions" #footer>
      <div class="w-full">
        <slot name="actions" />
      </div>
    </template>
  </UDashboardSidebar>
</template>

<script setup lang="ts">
const isOpen = defineModel<boolean>('open', { default: false })
</script>

<style>
.sidebar-wrapper {
  width: min(100%, 24rem);
}
</style>