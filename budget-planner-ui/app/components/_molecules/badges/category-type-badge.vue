<script setup lang="ts">
import { CategoryType } from "~/models/category/category";

defineProps<{
  type: CategoryType;
}>();
</script>

<template>
  <UBadge :color="type === CategoryType.INCOME ? 'success' : 'neutral'" variant="subtle" size="xs">
    {{ type }}
  </UBadge>
</template>
