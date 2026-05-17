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
  alt: 'Contact',
  size: 'md',
  ui: () => ({}),
  editable: false,
});

defineEmits(['edit']);

const {t} = useI18n();

const ICON_SIZE_CLASS: Record<NonNullable<Props['size']>, string> = {
  sm: 'size-4',
  md: 'size-4',
  lg: 'size-5',
  xl: 'size-6',
  '2xl': 'size-8',
  '3xl': 'size-10',
};

const iconSizeClass = computed(() => ICON_SIZE_CLASS[props.size]);
</script>

<template>
  <div class="relative inline-block group" v-bind="$attrs">
    <UAvatar
      :alt="alt"
      :icon="src ? undefined : 'i-lucide-user'"
      :size="size"
      :src="src ?? undefined"
      :ui="ui"
      class="ring-2 ring-primary/20"
    />
    <button
      v-if="editable"
      :aria-label="t('contacts.avatar.changeAria', {name: alt})"
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
