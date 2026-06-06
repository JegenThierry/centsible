<script lang="ts" setup>
import {computed, ref} from 'vue';
import {useCategoriesStore} from "~/stores/categoriesStore";
import AppButton from "~/components/_atoms/ui/app-button.vue";
import AppSelect from "~/components/_atoms/ui/app-select.vue";

const props = defineProps<{
  open: boolean;
  count: number;
}>();

const emit = defineEmits<{
  (e: 'update:open', value: boolean): void;
  (e: 'confirm', categoryId: number): void;
}>();

const {t} = useI18n();
const categoriesStore = useCategoriesStore();
const selected = ref<number | null>(null);

const items = computed(() =>
  categoriesStore.categories.map((c) => ({value: c.id, label: c.name})),
);

onMounted(() => {
  if (categoriesStore.categories.length === 0) categoriesStore.updateCategories();
});

function confirm() {
  if (selected.value === null) return;
  emit('confirm', selected.value);
  emit('update:open', false);
}
</script>

<template>
  <UModal :open="open" @update:open="(v) => emit('update:open', v)">
    <template #content>
      <div class="p-5 space-y-4 w-96 max-w-full">
        <h3 class="text-lg font-semibold">{{ t('transactions.bulk.recategorizeTitle') }}</h3>
        <p class="text-sm text-muted">{{ t('transactions.bulk.recategorizeBody', {count}) }}</p>
        <AppSelect v-model="selected"
                 :items="items"
                 :placeholder="t('transactions.selects.selectCategory')"
                 class="w-full"/>
        <div class="flex justify-end gap-2 pt-2">
          <AppButton color="neutral" variant="ghost" @click="emit('update:open', false)">
            {{ t('common.actions.cancel') }}
          </AppButton>
          <AppButton :disabled="selected === null" color="primary" @click="confirm">
            {{ t('transactions.bulk.recategorizeConfirm') }}
          </AppButton>
        </div>
      </div>
    </template>
  </UModal>
</template>
