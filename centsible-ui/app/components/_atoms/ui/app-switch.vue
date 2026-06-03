<script setup lang="ts">
</script>

<template>
  <USwitch>
    <template v-for="(_, name) in $slots" #[name]="slotProps">
      <slot :name="name" v-bind="slotProps ?? {}"/>
    </template>
  </USwitch>
</template>
