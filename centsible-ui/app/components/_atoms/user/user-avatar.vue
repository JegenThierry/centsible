<script lang="ts" setup>
interface Props {
  src?: string | null;
  alt?: string;
  size?: 'sm' | 'md' | 'lg' | 'xl' | '2xl' | '3xl';
  ui?: Record<string, unknown>;
  editable?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  src: null,
  alt: 'User Avatar',
  size: 'xl',
  ui: () => ({}),
  editable: false
});

const emit = defineEmits(['edit']);

const hoverClass = computed(() => props.src
  ? 'transition-opacity hover:opacity-80'
  : 'transition-colors hover:bg-elevated');

function onAvatarClick() {
  if (props.editable) emit('edit');
}
</script>

<template>
  <div class="relative inline-block" v-bind="$attrs">
    <UAvatar
      :alt="alt"
      :class="['cursor-pointer ring-2 ring-primary/20', hoverClass]"
      :icon="src ? undefined : 'i-lucide-user'"
      :size="size"
      :src="src ?? undefined"
      :ui="ui"
      @click="onAvatarClick"
    />
    <div v-if="editable" class="absolute bottom-1 right-1">
      <UButton
        class="rounded-full shadow-md"
        color="white"
        icon="i-lucide-camera"
        size="sm"
        square
        @click="emit('edit')"
      />
    </div>
  </div>
</template>
