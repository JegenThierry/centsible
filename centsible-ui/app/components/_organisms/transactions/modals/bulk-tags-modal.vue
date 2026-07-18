<script lang="ts" setup>
import {computed, ref} from 'vue';
import {useTagsStore} from "~/stores/tagsStore";
import AppButton from "~/components/_atoms/ui/app-button.vue";

const props = defineProps<{
  open: boolean;
  count: number;
  mode: 'add' | 'remove';
}>();

const emit = defineEmits<{
  (e: 'update:open', value: boolean): void;
  (e: 'confirm', tagIds: number[]): void;
}>();

const {t} = useI18n();
const tagsStore = useTagsStore();
const selected = ref<number[]>([]);

const title = computed(() => t(`transactions.bulk.${props.mode}TagsTitle`));
const body = computed(() => t(`transactions.bulk.${props.mode}TagsBody`, {count: props.count}));

onMounted(() => {
  if (tagsStore.tags.length === 0) tagsStore.fetchAll();
});

function toggleTag(id: number) {
  selected.value = selected.value.includes(id)
    ? selected.value.filter((x) => x !== id)
    : [...selected.value, id];
}

function isTagSelected(id: number): boolean {
  return selected.value.includes(id);
}

function confirm() {
  if (selected.value.length === 0) return;
  emit('confirm', selected.value);
  emit('update:open', false);
}
</script>

<template>
  <UModal :open="open" @update:open="(v) => emit('update:open', v)">
    <template #content>
      <div class="p-5 space-y-4 w-96 max-w-full">
        <h3 class="text-lg font-semibold">{{ title }}</h3>
        <p class="text-sm text-muted">{{ body }}</p>
        <div class="flex flex-wrap gap-2">
          <button v-for="tag in tagsStore.tags"
                  :key="tag.id"
                  type="button"
                  :class="isTagSelected(tag.id) ? 'ring-2 ring-primary-500' : 'opacity-60 hover:opacity-100'"
                  class="inline-flex items-center gap-1.5 rounded-full border border-default px-2.5 py-1 text-sm transition"
                  @click="toggleTag(tag.id)">
            <span class="w-2 h-2 rounded-full shrink-0" :style="{backgroundColor: tag.color}"/>
            {{ tag.name }}
          </button>
          <span v-if="tagsStore.tags.length === 0" class="text-xs text-muted">{{ t('transactions.filters.noTags') }}</span>
        </div>
        <div class="flex justify-end gap-2 pt-2">
          <AppButton color="neutral" variant="ghost" @click="emit('update:open', false)">
            {{ t('common.actions.cancel') }}
          </AppButton>
          <AppButton :disabled="selected.length === 0" color="primary" @click="confirm">
            {{ t('transactions.bulk.tagsConfirm') }}
          </AppButton>
        </div>
      </div>
    </template>
  </UModal>
</template>
