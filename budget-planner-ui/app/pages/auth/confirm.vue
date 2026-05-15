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

useHead({
  title: 'Confirm your account',
});

type ConfirmState = 'pending' | 'success' | 'error';

const CARDS: Record<ConfirmState, { icon: string; title: string; description: string; iconClass: string }> = {
  pending: {
    icon: 'i-lucide-loader-circle',
    title: 'Confirming your account…',
    description: 'Hang tight — this only takes a second.',
    iconClass: 'animate-spin text-primary',
  },
  success: {
    icon: 'i-lucide-circle-check',
    title: "You're all set",
    description: 'Your account is confirmed. Sign in to start tracking your money with Centsible.',
    iconClass: 'text-(--ui-success)',
  },
  error: {
    icon: 'i-lucide-circle-alert',
    title: 'This link is invalid or expired',
    description: "We couldn't confirm your account. Confirmation links expire after 24 hours — request a new one by registering again.",
    iconClass: 'text-(--ui-error)',
  },
};

const CTAS: Record<'success' | 'error', { icon: string; label: string }> = {
  success: {icon: 'i-lucide-log-in', label: 'Continue to sign in'},
  error: {icon: 'i-lucide-user-plus', label: 'Register again'},
};

const route = useRoute();
const api = useApi();
const state = ref<ConfirmState>('pending');

const card = computed(() => CARDS[state.value]);

onMounted(async () => {
  const token = route.query.token;
  if (typeof token !== 'string' || token.length === 0) {
    state.value = 'error';
    return;
  }

  try {
    state.value = (await useAuthService(api).confirm(token)) ? 'success' : 'error';
  } catch {
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
      class="max-w-xl mx-auto bg-white dark:bg-neutral-600"
      spotlight
      spotlight-color="primary">
      <template v-if="state !== 'pending'" #footer>
        <div class="flex flex-col sm:flex-row gap-3 sm:justify-end">
          <UButton color="neutral"
                   icon="i-lucide-home"
                   size="lg"
                   to="/"
                   variant="outline">
            Back to home
          </UButton>
          <UButton :icon="CTAS[state].icon"
                   color="primary"
                   size="lg"
                   to="/auth"
                   trailing-icon="i-lucide-arrow-right">
            {{ CTAS[state].label }}
          </UButton>
        </div>
      </template>
    </UPageCard>
  </UContainer>
</template>
