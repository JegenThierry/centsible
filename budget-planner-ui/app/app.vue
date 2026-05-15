<script lang="ts" setup>
import {useAuthStore} from "~/stores/authStore";
import TopNavBar from "~/components/_organisms/nav/top-nav-bar.vue";
import Footer from "~/components/_organisms/layout/footer.vue";
import SideNavBar from "~/components/_organisms/nav/side-nav-bar.vue";
import MainContentWrapper from "~/components/_wrapper/main-content-wrapper.vue";

const authStore = useAuthStore();
</script>

<template>
  <UApp>
    <div class="flex flex-1 h-screen overflow-hidden">
      <SideNavBar v-if="authStore.isAuthenticated"/>

      <MainContentWrapper>
        <template #header>
          <TopNavBar/>
        </template>

        <UMain>
          <NuxtLayout>
            <NuxtPage/>
          </NuxtLayout>
        </UMain>

        <template #footer>
          <Footer/>
        </template>
      </MainContentWrapper>
    </div>
  </UApp>
</template>
