<script lang="ts" setup>
import {useAuthStore} from "~/stores/authStore";
import TopNavBar from "~/components/_organisms/nav/top-nav-bar.vue";
import Footer from "~/components/_organisms/layout/footer.vue";
import SideNavBar from "~/components/_organisms/nav/side-nav-bar.vue";
import MainContentWrapper from "~/components/_wrapper/main-content-wrapper.vue";

const authStore = useAuthStore();
const router = useRouter();

onMounted(async () => {
  if (router.currentRoute.value.path !== '/') {
    return;
  }

  if (authStore.isAuthenticated) {
    navigateTo('/accounts');
    return;
  }

  navigateTo('/auth');
})
</script>

<template>
  <UApp>
    <div
      v-if="authStore.isAuthenticated"
      class="flex flex-1 h-screen overflow-hidden"
    >
      <SideNavBar/>

      <MainContentWrapper>
        <TopNavBar/>

        <div class="flex-1 overflow-y-auto">
          <UMain>
            <NuxtLayout>
              <NuxtPage/>
            </NuxtLayout>
          </UMain>
          <Footer/>
        </div>
      </MainContentWrapper>
    </div>

    <div v-else class="flex flex-col h-screen overflow-hidden bg-neutral-50/50 dark:bg-neutral-900">
      <UMain>
        <NuxtLayout>
          <NuxtPage/>
        </NuxtLayout>
      </UMain>
    </div>
  </UApp>
</template>
