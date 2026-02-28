<script setup lang="ts">
import {useAuthStore} from "~/stores/auth";
import TopNavBar from "~/components/_organisms/top-nav-bar.vue";
import Footer from "~/components/_organisms/footer.vue";

const authStore = useAuthStore();

onMounted(async () => {
  if (authStore.isAuthenticated) {
    navigateTo('/dashboard');
    return;
  }
  navigateTo('/login');
})

</script>

<template>
  <UApp>
    <TopNavBar/>

    <UMain>
      <NuxtLayout>
        <NuxtPage/>
      </NuxtLayout>
    </UMain>

    <Footer/>
  </UApp>
</template>
