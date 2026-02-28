<script setup lang="ts">

import CreateForm from "~/components/login/create-form.vue";

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
</script>

<template>
  <UContainer class="py-12">
    <UCard variant="subtle" class="max-w-xl mx-auto">
      <template #header>
        <h1>{{ activeHeader }}</h1>
      </template>
      <UTabs :items="tabs" v-model="activeTab">
        <template #login>
          <LoginForm></LoginForm>
        </template>
        <template #register>
          <CreateForm></CreateForm>
        </template>
      </UTabs>
    </UCard>
  </UContainer>
</template>

<style scoped>

</style>