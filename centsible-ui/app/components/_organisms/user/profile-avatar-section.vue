<script lang="ts" setup>
import UserAvatar from '~/components/_atoms/user/user-avatar.vue';
import AppButton from '~/components/_atoms/ui/app-button.vue';

interface Props {
  name: string;
  username: string;
  profilePicture?: string | null;
}

defineProps<Props>();
const emit = defineEmits<{
  (e: 'change', file: File): void;
}>();

const {t} = useI18n();

const fileInput = ref<HTMLInputElement | null>(null);

function onEditAvatar() {
  fileInput.value?.click();
}

function onFileChange(event: Event) {
  const target = event.target as HTMLInputElement;
  const file = target.files?.[0];
  if (!file) return;

  emit('change', file);
  target.value = '';
}
</script>

<template>
  <div class="flex flex-col items-center gap-6">
    <UserAvatar :alt="name"
                :src="profilePicture"
                editable
                size="3xl"
                @edit="onEditAvatar"/>

    <div class="flex flex-col items-center gap-2">
      <div class="text-center">
        <h2 class="text-xl font-semibold">{{ name }}</h2>
        <p class="text-gray-500">@{{ username }}</p>
      </div>

      <AppButton icon="i-lucide-camera"
                 :label="t('profile.avatar.change')"
                 variant="soft"
                 @click="onEditAvatar"/>
    </div>

    <input ref="fileInput"
           accept="image/*"
           class="hidden"
           type="file"
           @change="onFileChange"/>
  </div>
</template>
