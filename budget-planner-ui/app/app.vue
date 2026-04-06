<script setup lang="ts">
import {useAuthStore} from "~/stores/authStore";
import TopNavBar from "~/components/_organisms/top-nav-bar.vue";
import Footer from "~/components/_organisms/footer.vue";
import SideNavBar from "~/components/_organisms/nav/side-nav-bar.vue";

const authStore = useAuthStore();
const router = useRouter();

onMounted(async () => {
  if (authStore.isAuthenticated && router.currentRoute.value.path === '/') {
      navigateTo('/accounts');
      return;
  }

  if (router.currentRoute.value.path !== '/auth') {
    navigateTo('/auth');
  }
})
</script>

<template>
  <UApp>
    <SideNavBar/>
    <TopNavBar/>

    <UMain>
      <NuxtLayout>
        <NuxtPage/>
      </NuxtLayout>
    </UMain>
    <Footer/>


  </UApp>
</template>
