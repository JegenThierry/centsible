<script setup lang="ts">
</script>

<template>
  <URadioGroup>
    <template v-for="(_, name) in $slots" #[name]="slotProps">
      <slot :name="name" v-bind="slotProps ?? {}"/>
    </template>
  </URadioGroup>
</template>
