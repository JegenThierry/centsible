<template>
  <div class="relative inline-block" v-bind="$attrs">
    <UAvatar
      v-if="src"
      :alt="alt"
      :size="size"
      :src="src"
      :ui="ui"
      class="cursor-pointer hover:opacity-80 transition-opacity ring-2 ring-primary/20"
      @click="editable && $emit('edit')"
    />
    <UAvatar
      v-else
      :alt="alt"
      :size="size"
      :ui="ui"
      class="cursor-pointer hover:bg-neutral-100 dark:hover:bg-neutral-800 transition-colors ring-2 ring-primary/20"
      icon="i-heroicons-user"
      @click="editable && $emit('edit')"
    />
    <div v-if="editable" class="absolute bottom-1 right-1">
      <UButton
        class="rounded-full shadow-md"
        color="white"
        icon="i-heroicons-camera"
        size="sm"
        square
        @click="$emit('edit')"
      />
    </div>
  </div>
</template>

<script lang="ts" setup>
interface Props {
  src?: string | null;
  alt?: string;
  size?: 'sm' | 'md' | 'lg' | 'xl' | '2xl' | '3xl';
  ui?: any;
  editable?: boolean;
}

withDefaults(defineProps<Props>(), {
  src: null,
  alt: 'User Avatar',
  size: 'xl',
  ui: () => ({}),
  editable: false
});

defineEmits(['edit']);
</script>
