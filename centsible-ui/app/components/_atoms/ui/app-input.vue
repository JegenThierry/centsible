<script setup lang="ts">
</script>

<template>
  <UInput>
    <template v-for="(_, name) in $slots" #[name]="slotProps">
      <slot :name="name" v-bind="slotProps ?? {}"/>
    </template>
  </UInput>
</template>
