<script setup lang="ts">
import {useAuthStore} from "~/stores/authStore";
import TopNavBar from "~/components/_organisms/top-nav-bar.vue";
import Footer from "~/components/_organisms/footer.vue";

const authStore = useAuthStore();
const router = useRouter();

onMounted(async () => {
  if(router.currentRoute.value.path !== '/') {
    return;
  }
  if (authStore.isAuthenticated) {
    navigateTo('/dashboard');
    return;
  }
  navigateTo('/auth');
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
