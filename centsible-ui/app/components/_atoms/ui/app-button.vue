<script setup lang="ts">
</script>

<template>
  <UButton>
    <template v-for="(_, name) in $slots" #[name]="slotProps">
      <slot :name="name" v-bind="slotProps ?? {}"/>
    </template>
  </UButton>
</template>
