<script lang="ts" setup>
import CreateForm from "~/components/_organisms/auth/create-form.vue";
import LoginForm from "~/components/_organisms/auth/login-form.vue";

const {t} = useI18n();

const tabs = computed(() => [
  {
    label: t('auth.tabs.login'),
    icon: 'i-lucide-user',
    slot: 'login' as const,
    value: 'login' as const,
  },
  {
    label: t('auth.tabs.register'),
    icon: 'i-lucide-lock',
    slot: 'register' as const,
    value: 'register' as const,
  }
]);

const activeTab = ref<'login' | 'register'>('login');
const activeHeader = computed(() =>
  activeTab.value === 'login' ? t('auth.login.title') : t('auth.register.title')
);
const activeDescription = computed(() =>
  activeTab.value === 'login' ? t('auth.login.description') : t('auth.register.description')
);
const activeIcon = computed(() =>
  activeTab.value === 'login' ? 'i-lucide-log-in' : 'i-lucide-user-plus'
);
</script>

<template>
  <UContainer class="py-12">
    <UPageCard
      :description="activeDescription"
      :icon="activeIcon"
      :title="activeHeader"
      class="max-w-xl mx-auto"
      spotlight
      spotlight-color="primary"
    >
      <UTabs v-model="activeTab" :items="tabs" class="w-full">
        <template #login>
          <div class="pt-4">
            <LoginForm v-if="activeTab === 'login'"/>
          </div>
        </template>
        <template #register>
          <div class="pt-4">
            <CreateForm v-if="activeTab === 'register'"/>
          </div>
        </template>
      </UTabs>
    </UPageCard>
  </UContainer>
</template>
