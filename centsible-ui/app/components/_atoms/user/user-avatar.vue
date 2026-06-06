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
  if (!props.editable) {
    return;
  }
  emit('edit');
}
</script>

<template>
  <div class="relative inline-block" v-bind="$attrs">
    <UAvatar :alt="alt"
             :class="['cursor-pointer ring-2 ring-primary/20', hoverClass]"
             :icon="src ? undefined : 'i-lucide-user'"
             :size="size"
             :src="src ?? undefined"
             :ui="ui"
             @click="onAvatarClick"/>
  </div>
</template>
