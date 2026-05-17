<script lang="ts" setup>
import {computed} from "vue";
import type {UserDto} from "~/models/user/user-dto";
import {useAuthStore} from "~/stores/authStore";

defineProps<{
  user: UserDto,
}>()

const authStore = useAuthStore();
const {t} = useI18n();

const items = computed(() => [
  [
    {
      label: t('profile.dropdown.profile'),
      icon: 'i-lucide-user',
      onSelect: () => navigateTo('/profile')
    },
    {
      label: t('profile.dropdown.settings'),
      icon: 'i-lucide-settings',
      onSelect: () => navigateTo('/settings')
    },
    {
      label: t('profile.dropdown.about'),
      icon: 'i-lucide-info',
      onSelect: () => navigateTo('/about')
    }
  ],
  [
    {
      label: t('profile.dropdown.logout'),
      icon: 'i-lucide-log-out',
      color: 'error' as any,
      onSelect: () => authStore.logout()
    }
  ]
])

</script>

<template>
  <UDropdownMenu :content="{ align: 'end' }" :items="items">
    <UButton class="h-auto cursor-pointer" color="neutral" variant="ghost">
      <UUser
        :avatar="{
            src: user.profilePicture || undefined,
            icon: user.profilePicture ? undefined : 'i-heroicons-user'
          }"
        :description="user.email"
        :name="user.name"
      />
      <UIcon class="text-xl ml-1" name="i-lucide-chevron-down"/>
    </UButton>
  </UDropdownMenu>
</template>

<style scoped>

</style>
