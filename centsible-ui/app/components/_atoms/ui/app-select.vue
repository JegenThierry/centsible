<script setup lang="ts">
</script>

<template>
  <USelect>
    <template v-for="(_, name) in $slots" #[name]="slotProps">
      <slot :name="name" v-bind="slotProps ?? {}"/>
    </template>
  </USelect>
</template>
