<script lang="ts" setup>
import PageHeader from "~/components/_molecules/page/page-header.vue";
import {useUserStore} from '~/stores/userStore';
import UserAvatar from '~/components/_atoms/user/user-avatar.vue';
import ProfileForm from '~/components/_organisms/user/profile-form.vue';
import {useUserNotifications} from "~/components/_organisms/user/notifcations";
import type {UserProfileForm} from "~/models/user/user-profile-form";

const userStore = useUserStore();
const {
  onAvatarUpdateSuccess,
  onAvatarUpdateError,
  onProfileUpdateSuccess,
  onProfileUpdateError,
  onValidationError
} = useUserNotifications();

const fileInput = ref<HTMLInputElement | null>(null);
const initialFormValues = ref<UserProfileForm>();
const pending = ref(false);

onMounted(async () => {
  if (userStore.user) return;

  const user = userStore.user;
  if (!user) return;

  const {firstName, lastName, email} = user;

  initialFormValues.value = {
    firstName,
    lastName,
    email,
  };
});

async function onSaveProfile(data: any) {
  pending.value = true;
  try {
    await userStore.updateProfile(data);
    onProfileUpdateSuccess();
  } catch (error: any) {
    onProfileUpdateError(error.message);
  }
}

function onEditAvatar() {
  fileInput.value?.click();
}

async function onFileChange(event: Event) {
  const target = event.target as HTMLInputElement;
  const file = target.files?.[0];
  if (!file) return;

  try {
    await userStore.updateProfilePicture(file);
    onAvatarUpdateSuccess();
  } catch (error: any) {
    onAvatarUpdateError(error.message);
  }
}
</script>

<template>
  <UContainer class="py-6 sm:py-10">
    <PageHeader description="Manage your account settings and profile information"
                title="Profile"/>

    <div v-if="userStore.pending && !userStore.user" class="flex justify-center py-8">
      <UIcon class="w-8 h-8 animate-spin text-primary" name="i-heroicons-arrow-path"/>
    </div>

    <UCard v-else-if="userStore.user" class="max-w-2xl mx-auto" variant="soft">
      <div class="space-y-12 mt-8">
        <div class="flex flex-col items-center gap-6">
          <div class="relative">
            <UserAvatar :alt="userStore.user.name"
                        :src="userStore.user.profilePicture"
                        editable
                        size="3xl"
                        @edit="onEditAvatar"/>
          </div>

          <div class="flex flex-col items-center gap-2">
            <div class="text-center">
              <h2 class="text-xl font-semibold">{{ userStore.user.name }}</h2>
              <p class="text-gray-500">@{{ userStore.user.username }}</p>
            </div>

            <UButton icon="i-heroicons-camera"
                     label="Change Picture"
                     variant="soft"
                     @click="onEditAvatar"/>
          </div>

          <input ref="fileInput"
                 accept="image/*"
                 class="hidden"
                 type="file"
                 @change="onFileChange"/>
        </div>

        <div class="max-w-2xl mx-auto border-t border-gray-100 dark:border-gray-800 pt-2">
          <ProfileForm :initial-values="initialFormValues"
                       :loading="userStore.pending"
                       @save="onSaveProfile"
                       @validation-failed="onValidationError"/>
        </div>
      </div>
    </UCard>
  </UContainer>
</template>
