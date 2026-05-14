<script lang="ts" setup>
interface Props {
  src?: string | null;
  alt?: string;
  size?: 'sm' | 'md' | 'lg' | 'xl' | '2xl' | '3xl';
  ui?: any;
  editable?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  src: null,
  alt: 'Contact',
  size: 'md',
  ui: () => ({}),
  editable: false,
});

defineEmits(['edit']);

const iconSizeClass = computed(() => {
  switch (props.size) {
    case 'sm':
    case 'md':
      return 'size-4';
    case 'lg':
      return 'size-5';
    case 'xl':
      return 'size-6';
    case '2xl':
      return 'size-8';
    case '3xl':
      return 'size-10';
    default:
      return 'size-5';
  }
});
</script>

<template>
  <div class="relative inline-block group" v-bind="$attrs">
    <UAvatar
      v-if="src"
      :alt="alt"
      :size="size"
      :src="src"
      :ui="ui"
      class="ring-2 ring-primary/20"
    />
    <UAvatar
      v-else
      :alt="alt"
      :size="size"
      :ui="ui"
      class="ring-2 ring-primary/20"
      icon="i-lucide-user"
    />
    <button
      v-if="editable"
      :aria-label="`Change picture for ${alt}`"
      class="absolute inset-0 rounded-full flex items-center justify-center cursor-pointer bg-black/0 hover:bg-black/50 focus-visible:bg-black/50 focus:outline-none transition-colors"
      type="button"
      @click="$emit('edit')"
    >
      <UIcon
        :class="iconSizeClass"
        class="text-white opacity-0 group-hover:opacity-100 focus-visible:opacity-100 transition-opacity"
        name="i-lucide-camera"
      />
    </button>
  </div>
</template>
