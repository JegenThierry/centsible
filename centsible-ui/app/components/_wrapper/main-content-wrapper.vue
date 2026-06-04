<script lang="ts" setup>
defineProps<{
  inset?: boolean
}>()
</script>

<template>
  <div class="flex-1 flex flex-col overflow-hidden lg:peer-data-[variant=floating]:my-4 sm:peer-data-[variant=inset]:m-4 lg:peer-data-[variant=inset]:not-peer-data-[collapsible=offcanvas]:ms-0 sm:peer-data-[variant=inset]:rounded-xl sm:peer-data-[variant=inset]:shadow-sm sm:peer-data-[variant=inset]:ring sm:peer-data-[variant=inset]:ring-default">
    <slot name="header"/>
    <div class="flex-1 overflow-y-auto">
      <slot/>
      <slot name="footer"/>
    </div>
  </div>
</template>
