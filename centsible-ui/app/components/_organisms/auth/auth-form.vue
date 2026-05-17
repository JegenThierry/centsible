<script lang="ts" setup>
import CreateForm from "~/components/_organisms/auth/create-form.vue";
import LoginForm from "~/components/_organisms/auth/login-form.vue";

const {t} = useI18n();

type TabKey = 'login' | 'register';
const TAB_META: Record<TabKey, {tabIcon: string; cardIcon: string}> = {
  login:    {tabIcon: 'i-lucide-user', cardIcon: 'i-lucide-log-in'},
  register: {tabIcon: 'i-lucide-lock', cardIcon: 'i-lucide-user-plus'},
};

const tabs = computed(() => (Object.keys(TAB_META) as TabKey[]).map((value) => ({
  label: t(`auth.tabs.${value}`),
  icon: TAB_META[value].tabIcon,
  slot: value,
  value,
})));

const activeTab = ref<TabKey>('login');
const activeHeader = computed(() => t(`auth.${activeTab.value}.title`));
const activeDescription = computed(() => t(`auth.${activeTab.value}.description`));
const activeIcon = computed(() => TAB_META[activeTab.value].cardIcon);
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
