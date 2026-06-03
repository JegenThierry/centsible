<script setup lang="ts">
</script>

<template>
  <UCheckbox>
    <template v-for="(_, name) in $slots" #[name]="slotProps">
      <slot :name="name" v-bind="slotProps ?? {}"/>
    </template>
  </UCheckbox>
</template>
