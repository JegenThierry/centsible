<script lang="ts" setup>
import {CategoryType} from "~/models/category/category";

defineProps<{
  type: CategoryType;
}>();

const {t} = useI18n();
</script>

<template>
  <UBadge :color="type === CategoryType.INCOME ? 'success' : 'error'" size="xs" variant="subtle">
    {{ t(`transactions.category.type.${type}`) }}
  </UBadge>
</template>
