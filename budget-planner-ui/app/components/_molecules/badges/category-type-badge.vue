<script lang="ts" setup>
import {CategoryType} from "~/models/category/category";

defineProps<{
  type: CategoryType;
}>();
</script>

<template>
  <UBadge :color="type === CategoryType.INCOME ? 'success' : 'error'" size="xs" variant="subtle">
    {{ type }}
  </UBadge>
</template>
