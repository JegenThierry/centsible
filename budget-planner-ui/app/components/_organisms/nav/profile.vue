<script lang="ts" setup>
import {computed} from "vue";
import type {UserDto} from "~/models/user/user-dto";
import {useAuthStore} from "~/stores/authStore";

defineProps<{
  user: UserDto,
}>()

const authStore = useAuthStore();

const items = computed(() => [
  [
    {
      label: 'Profile',
      icon: 'i-lucide-user',
      onSelect: () => navigateTo('/profile')
    },
    {
      label: 'Settings',
      icon: 'i-lucide-settings',
      onSelect: () => navigateTo('/settings')
    },
    {
      label: 'About',
      icon: 'i-lucide-info',
      onSelect: () => navigateTo('/about')
    }
  ],
  [
    {
      label: 'Logout',
      icon: 'i-lucide-log-out',
      color: 'error' as any,
      onSelect: () => authStore.logout()
    }
  ]
])

</script>

<template>
  <UDropdownMenu :content="{ align: 'end' }" :items="items">
    <UButton color="neutral" variant="ghost" class="h-auto cursor-pointer">
      <UUser
        :avatar="{
            src: user.profilePicture || undefined,
            icon: user.profilePicture ? undefined : 'i-heroicons-user'
          }"
        :description="user.email"
        :name="user.name"
      />
      <UIcon name="i-lucide-chevron-down" class="text-xl ml-1" />
    </UButton>
  </UDropdownMenu>
</template>

<style scoped>

</style>
