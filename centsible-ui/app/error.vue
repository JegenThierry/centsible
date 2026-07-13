<script lang="ts" setup>
import {computed} from 'vue';
import type {NuxtError} from '#app';
import AppButton from "~/components/_atoms/ui/app-button.vue";

const props = defineProps<{ error: NuxtError }>();

const {t} = useI18n();

const isNotFound = computed(() => props.error?.statusCode === 404);
const title = computed(() =>
  isNotFound.value ? t('common.errorPage.notFoundTitle') : t('common.errorPage.errorTitle'),
);
const description = computed(() =>
  isNotFound.value ? t('common.errorPage.notFoundDescription') : t('common.errorPage.errorDescription'),
);

useHead({title});

function goHome() {
  clearError({redirect: '/'});
}
</script>

<template>
  <UApp>
    <main class="min-h-screen flex flex-col items-center justify-center gap-4 px-4 text-center">
      <p aria-hidden="true" class="text-7xl font-bold text-primary">{{ error?.statusCode ?? 500 }}</p>
      <h1 class="text-2xl font-semibold">{{ title }}</h1>
      <p class="text-muted max-w-md">{{ description }}</p>
      <AppButton icon="i-lucide-house" size="lg" @click="goHome">
        {{ t('common.errorPage.backHome') }}
      </AppButton>
    </main>
  </UApp>
</template>
