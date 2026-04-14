<script setup lang="ts">
import PageHeader from "~/components/_molecules/page/page-header.vue";
import {useUserStore} from '~/stores/userStore';
import UserAvatar from '~/components/_atoms/user/user-avatar.vue';
import ProfileForm from '~/components/_organisms/user/profile-form.vue';
import {useToasts} from '~/services/toasts/toast-service';
import {useUserNotifications} from "~/components/_organisms/user/notifcations";

const userStore = useUserStore();
const {success, error: showError} = useToasts();
const {
  onAvatarUpdateSuccess,
  onAvatarUpdateError,
  onProfileUpdateSuccess,
  onProfileUpdateError,
  onValidationError
} = useUserNotifications();

const fileInput = ref<HTMLInputElement | null>(null);
const pending = ref(false);

onMounted(() => {
  if (userStore.user) return;
  userStore.fetchMyself();
});

async function onSaveProfile(data: any) {
  pending.value = true;
  try {
    await userStore.updateProfile(data);
    onProfileUpdateSuccess();
  } catch (error: any) {
    onProfileUpdateError(error.message);
  } finally {
    pending.value = false;
  }
}

function onEditAvatar() {
  fileInput.value?.click();
}

async function onFileChange(event: Event) {
  const target = event.target as HTMLInputElement;
  const file = target.files?.[0];
  if (!file) return;

  pending.value = true;
  try {
    await userStore.updateProfilePicture(file);
    onAvatarUpdateSuccess();
  } catch (error: any) {
    onAvatarUpdateError(error.message);
  } finally {
    pending.value = false;
  }
}
</script>

<template>
  <UContainer class="py-6 sm:py-10">
    <PageHeader title="Profile"
                description="Manage your account settings and profile information"/>

    <div v-if="userStore.pending && !userStore.user" class="flex justify-center py-8">
      <UIcon name="i-heroicons-arrow-path" class="w-8 h-8 animate-spin text-primary"/>
    </div>

    <UCard v-else-if="userStore.user" variant="soft" class="max-w-2xl mx-auto">
      <div class="space-y-12 mt-8">
        <div class="flex flex-col items-center gap-6">
          <div class="relative">
            <UserAvatar :src="userStore.user.profilePicture"
                        :alt="userStore.user.name"
                        size="3xl"
                        editable
                        @edit="onEditAvatar"/>
          </div>

          <div class="flex flex-col items-center gap-2">
            <div class="text-center">
              <h2 class="text-xl font-semibold">{{ userStore.user.name }}</h2>
              <p class="text-gray-500">@{{ userStore.user.username }}</p>
            </div>

            <UButton label="Change Picture"
                     variant="soft"
                     icon="i-heroicons-camera"
                     @click="onEditAvatar"/>
          </div>

          <input ref="fileInput"
                 type="file"
                 accept="image/*"
                 class="hidden"
                 @change="onFileChange"/>
        </div>

        <div class="max-w-2xl mx-auto border-t border-gray-100 dark:border-gray-800 pt-2">
          <ProfileForm :initial-values="{
                         firstName: userStore.user.firstName,
                         lastName: userStore.user.lastName,
                         email: userStore.user.email
                       }"
                       :loading="userStore.pending"
                       @save="onSaveProfile"
                       @validation-failed="onValidationError"/>
        </div>
      </div>
    </UCard>
  </UContainer>
</template>
