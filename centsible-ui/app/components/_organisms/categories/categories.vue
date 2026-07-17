<script lang="ts" setup>
import {useCategoriesStore} from "~/stores/categoriesStore";
const CategoryModal = defineAsyncComponent(() => import("~/components/_organisms/categories/modals/category-modal.vue"));
const DeleteCategoryModal = defineAsyncComponent(() => import("~/components/_organisms/categories/modals/delete-category-modal.vue"));
import CategoryCard from "~/components/_molecules/cards/category-card.vue";
import LoadingAnimation from "~/components/_atoms/animations/loading-animation.vue";
import CardSkeleton from "~/components/_molecules/skeletons/card-skeleton.vue";
import PageHeader from "~/components/_molecules/page/page-header.vue";
import AppEmptyState from "~/components/_molecules/feedback/app-empty-state.vue";
import AppButton from "~/components/_atoms/ui/app-button.vue";
import type {Category} from "~/models/category/category";

const categoriesStore = useCategoriesStore();
const {t} = useI18n();

const systemCategories = computed(() => categoriesStore.categories.filter(c => c.system));
const userCategories = computed(() => categoriesStore.categories.filter(c => !c.system));

const isCreateModalOpen = ref(false);
const isEditModalOpen = ref(false);
const isDeleteModalOpen = ref(false);
const selectedCategory = ref<Category>();

function openEditModal(category: Category) {
  selectedCategory.value = category;
  isEditModalOpen.value = true;
}

function openDeleteModal(category: Category) {
  selectedCategory.value = category;
  isDeleteModalOpen.value = true;
}

onMounted(() => {
  categoriesStore.updateCategories();
});
</script>

<template>
  <UContainer class="py-6 sm:py-10">
    <PageHeader
      :description="t('categories.page.description')"
      :title="t('categories.page.title')"
    >
      <template #actions>
        <AppButton class="w-full sm:w-auto justify-center"
                   icon="i-lucide-plus"
                   @click="isCreateModalOpen = true">
          {{ t('categories.page.createAction') }}
        </AppButton>
      </template>
    </PageHeader>

    <div v-if="categoriesStore.pending && categoriesStore.categories.length > 0" class="flex justify-center mb-6">
      <LoadingAnimation/>
    </div>

    <AppEmptyState
      v-if="categoriesStore.categories.length === 0 && !categoriesStore.pending"
      :description="t('categories.empty.description')"
      icon="i-lucide-tag"
      :title="t('categories.empty.title')"
    >
      <template #actions>
        <AppButton class="w-full sm:w-auto justify-center" @click="isCreateModalOpen = true">
          {{ t('categories.empty.action') }}
        </AppButton>
      </template>
    </AppEmptyState>

    <div v-else class="space-y-12">
      <template v-if="categoriesStore.pending && categoriesStore.categories.length === 0">
        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 sm:gap-6">
          <CardSkeleton v-for="i in 6" :key="i"/>
        </div>
      </template>

      <section v-if="userCategories.length > 0">
        <h2 class="text-xl font-semibold mb-6 flex items-center gap-2">
          <UIcon class="w-5 h-5 text-primary-500" name="i-lucide-user"/>
          {{ t('categories.sections.yours') }}
        </h2>
        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 sm:gap-6">
          <CategoryCard v-for="category in userCategories"
                        :key="category.id"
                        :category="category"
                        @delete="openDeleteModal"
                        @edit="openEditModal"/>
        </div>
      </section>

      <section v-if="systemCategories.length > 0">
        <h2 class="text-xl font-semibold mb-6 flex items-center gap-2 text-neutral-600 dark:text-neutral-400">
          <UIcon class="w-5 h-5" name="i-lucide-settings"/>
          {{ t('categories.sections.system') }}
        </h2>
        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 sm:gap-6">
          <CategoryCard v-for="category in systemCategories"
                        :key="category.id"
                        :category="category"/>
        </div>
      </section>
    </div>

    <CategoryModal v-model:open="isCreateModalOpen"/>
    <CategoryModal v-model:open="isEditModalOpen" :category="selectedCategory"/>
    <DeleteCategoryModal v-model:open="isDeleteModalOpen" :category="selectedCategory"/>
  </UContainer>
</template>
