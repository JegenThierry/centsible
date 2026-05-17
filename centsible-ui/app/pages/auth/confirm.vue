<script lang="ts" setup>
import {useAuthService} from "~/services/auth/auth-service";

definePageMeta({
  middleware: [
    () => {
      const authStore = useAuthStore();
      if (authStore.isAuthenticated) {
        return navigateTo('/accounts');
      }
    },
  ],
});

const {t} = useI18n();
useHead({
  title: t('auth.pageTitle.confirm'),
});

type ConfirmState = 'pending' | 'success' | 'error';

const CARD_META: Record<ConfirmState, { icon: string; iconClass: string }> = {
  pending: {
    icon: 'i-lucide-loader-circle',
    iconClass: 'animate-spin text-primary',
  },
  success: {
    icon: 'i-lucide-circle-check',
    iconClass: 'text-(--ui-success)',
  },
  error: {
    icon: 'i-lucide-circle-alert',
    iconClass: 'text-(--ui-error)',
  },
};

const CTA_ICONS: Record<'success' | 'error', string> = {
  success: 'i-lucide-log-in',
  error: 'i-lucide-user-plus',
};

const route = useRoute();
const api = useApi();
const state = ref<ConfirmState>('pending');

const card = computed(() => ({
  ...CARD_META[state.value],
  title: t(`auth.confirm.${state.value}Title`),
  description: t(`auth.confirm.${state.value}Description`),
}));

const ctaLabel = computed(() =>
  state.value === 'success' ? t('auth.confirm.continueToSignIn') : t('auth.confirm.registerAgain')
);

onMounted(async () => {
  const token = route.query.token;
  if (typeof token !== 'string' || token.length === 0) {
    state.value = 'error';
    return;
  }

  try {
    state.value = (await useAuthService(api).confirm(token)) ? 'success' : 'error';
  } catch (error) {
    console.error('Confirm token request failed', error);
    state.value = 'error';
  }
});
</script>

<template>
  <UContainer class="py-12">
    <UPageCard
      :description="card.description"
      :icon="card.icon"
      :title="card.title"
      :ui="{ leadingIcon: card.iconClass }"
      class="max-w-xl mx-auto"
      spotlight
      spotlight-color="primary">
      <template v-if="state !== 'pending'" #footer>
        <div class="flex flex-col sm:flex-row gap-3 sm:justify-end">
          <UButton color="neutral"
                   icon="i-lucide-home"
                   size="lg"
                   to="/"
                   variant="outline">
            {{ t('auth.confirm.backToHome') }}
          </UButton>
          <UButton :icon="CTA_ICONS[state]"
                   color="primary"
                   size="lg"
                   to="/auth"
                   trailing-icon="i-lucide-arrow-right">
            {{ ctaLabel }}
          </UButton>
        </div>
      </template>
    </UPageCard>
  </UContainer>
</template>
