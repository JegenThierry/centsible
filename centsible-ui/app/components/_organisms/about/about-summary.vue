<script lang="ts" setup>
import type {SystemInformation} from "~/models/system/system-information";

defineProps<{
  info: SystemInformation;
}>();
</script>

<template>
  <UCard class="mb-6" variant="soft">
    <div class="flex flex-col sm:flex-row sm:items-center gap-4 sm:gap-6">
      <img class="w-16 h-16 rounded-2xl" src="/brand/favicon.svg" alt=""/>
      <div class="flex-1">
        <div class="flex flex-wrap items-center gap-3">
          <h2 class="text-xl sm:text-2xl font-semibold text-highlighted">{{ info.name }}</h2>
          <UBadge color="primary" variant="subtle" :label="`v${info.version}`"/>
        </div>
        <p v-if="info.description" class="mt-2 text-sm sm:text-base text-muted">
          {{ info.description }}
        </p>
      </div>
    </div>
  </UCard>
</template>
