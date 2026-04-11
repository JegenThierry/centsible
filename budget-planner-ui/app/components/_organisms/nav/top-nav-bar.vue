<script setup lang="ts">

import Profile from "~/components/_organisms/nav/profile.vue";
import ProfileSkeleton from "~/components/_molecules/skeletons/profile-skeleton.vue";
import {useUserStore} from "~/stores/userStore";
import {useAuthStore} from "~/stores/authStore";
import {useSidebar} from "~/composables/use-sidebar";

const userStore = useUserStore();
const authStore = useAuthStore();
const {open} = useSidebar();

const currentPanelIcon = computed(() => open.value ? 'i-lucide-panel-left' : 'i-lucide-panel-right');

function onToggleOpen() {
  open.value = !open.value;
}

onMounted(async () => {
  if (!authStore.isAuthenticated) {
    return;
  }

  await userStore.fetchMyself();
});
</script>

<template>
  <div class="h-(--ui-header-height) shrink-0 flex items-center px-4 border-b border-default">
    <div class="flex-1 flex items-center gap-2">
      <UButton :icon="currentPanelIcon"
               color="neutral"
               variant="ghost"
               aria-label="Toggle sidebar"
               @click="onToggleOpen()"/>
    </div>

    <div class="flex items-center gap-2">
      <UColorModeButton />
      <Profile v-if="userStore.user" :user="userStore.user" />
      <ProfileSkeleton v-else-if="authStore.isAuthenticated" />
    </div>
  </div>
</template>
