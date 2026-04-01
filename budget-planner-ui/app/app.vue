<script setup lang="ts">
import {useAuthStore} from "~/stores/authStore";
import TopNavBar from "~/components/_organisms/top-nav-bar.vue";
import Footer from "~/components/_organisms/footer.vue";

const authStore = useAuthStore();
const router = useRouter();

onMounted(async () => {
  if (authStore.isAuthenticated) {
    if (router.currentRoute.value.path === '/') {
      navigateTo('/accounts');
    }
  } else if (router.currentRoute.value.path !== '/auth') {
    navigateTo('/auth');
  }
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
