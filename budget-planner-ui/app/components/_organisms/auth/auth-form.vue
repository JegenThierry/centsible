<script lang="ts" setup>
import CreateForm from "~/components/_organisms/auth/create-form.vue";
import LoginForm from "~/components/_organisms/auth/login-form.vue";

const tabs = [
  {
    label: 'Login',
    icon: 'i-lucide-user',
    slot: 'login',
    value: 'login',
  },
  {
    label: 'Register',
    icon: 'i-lucide-lock',
    slot: 'register',
    value: 'register',
  }
]

const activeTab = ref<'login' | 'register'>('login');
const activeHeader = computed(() => {
  switch (activeTab.value) {
    case 'login':
      return 'Welcome back!';
    case 'register':
      return 'Register!';
  }
})

const activeDescription = computed(() => {
  return activeTab.value === 'login'
    ? 'Login to your account to manage your budget.'
    : 'Create a new account to start planning your finances.';
})

const activeIcon = computed(() => {
  return activeTab.value === 'login' ? 'i-lucide-log-in' : 'i-lucide-user-plus';
})
</script>

<template>
  <UContainer class="py-12">
    <UPageCard
      :description="activeDescription"
      :icon="activeIcon"
      :title="activeHeader"
      class="max-w-xl mx-auto bg-white dark:bg-neutral-600"
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
