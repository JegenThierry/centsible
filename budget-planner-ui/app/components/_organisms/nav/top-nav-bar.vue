<script lang="ts" setup>

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
      <UButton v-if="authStore.isAuthenticated"
               :icon="currentPanelIcon"
               aria-label="Toggle sidebar"
               color="neutral"
               variant="ghost"
               @click="onToggleOpen()"/>
      <NuxtLink v-else
                class="flex items-center gap-2 font-semibold text-highlighted hover:text-primary transition-colors"
                to="/">
        <UIcon class="w-5 h-5 text-primary" name="i-lucide-wallet"/>
        Budget Planner
      </NuxtLink>
    </div>

    <div class="flex items-center gap-2">
      <UColorModeButton/>
      <template v-if="authStore.isAuthenticated">
        <Profile v-if="userStore.user" :user="userStore.user"/>
        <ProfileSkeleton v-else/>
      </template>
      <UButton v-else
               color="primary"
               icon="i-lucide-log-in"
               to="/auth">
        Sign in
      </UButton>
    </div>
  </div>
</template>
