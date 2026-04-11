<script lang="ts" setup>
import {useCategoriesStore} from "~/stores/categoriesStore";
import CreateCategoryModal from "~/components/_organisms/categories/modals/create-category-modal.vue";
import EditCategoryModal from "~/components/_organisms/categories/modals/edit-category-modal.vue";
import DeleteCategoryModal from "~/components/_organisms/categories/modals/delete-category-modal.vue";
import CategoryCard from "~/components/_organisms/cards/category-card.vue";
import LoadingAnimation from "~/components/_atoms/animations/loading-animation.vue";
import CardSkeleton from "~/components/_molecules/skeletons/card-skeleton.vue";
import PageHeader from "~/components/_molecules/page/page-header.vue";
import AppEmptyState from "~/components/_molecules/feedback/app-empty-state.vue";
import type {Category} from "~/models/category/category";

const categoriesStore = useCategoriesStore();

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
      title="Categories"
      description="Manage your income and expense categories"
    >
      <template #actions>
        <UButton icon="i-lucide-plus" class="w-full sm:w-auto justify-center" @click="isCreateModalOpen = true">Create Category</UButton>
      </template>
    </PageHeader>

    <div v-if="categoriesStore.pending && categoriesStore.categories.length > 0" class="flex justify-center mb-6">
      <LoadingAnimation />
    </div>

    <AppEmptyState
      v-if="categoriesStore.categories.length === 0 && !categoriesStore.pending"
      icon="i-lucide-tag"
      title="No categories found"
      description="Create your first category to start tracking your budget."
    >
      <template #actions>
        <UButton class="w-full sm:w-auto justify-center" @click="isCreateModalOpen = true">Create Category</UButton>
      </template>
    </AppEmptyState>

    <div v-else class="space-y-12">
      <template v-if="categoriesStore.pending && categoriesStore.categories.length === 0">
        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 sm:gap-6">
          <CardSkeleton v-for="i in 6" :key="i" />
        </div>
      </template>

      <section v-if="userCategories.length > 0">
        <h2 class="text-xl font-semibold mb-6 flex items-center gap-2">
          <UIcon name="i-lucide-user" class="w-5 h-5 text-primary-500" />
          Your Categories
        </h2>
        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 sm:gap-6">
          <CategoryCard v-for="category in userCategories"
                        :key="category.id"
                        :category="category"
                        @edit="openEditModal"
                        @delete="openDeleteModal" />
        </div>
      </section>

      <section v-if="systemCategories.length > 0">
        <h2 class="text-xl font-semibold mb-6 flex items-center gap-2 text-neutral-600 dark:text-neutral-400">
          <UIcon name="i-lucide-settings" class="w-5 h-5" />
          System Categories
        </h2>
        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 sm:gap-6">
          <CategoryCard v-for="category in systemCategories"
                        :key="category.id"
                        :category="category" />
        </div>
      </section>
    </div>

    <CreateCategoryModal v-model:open="isCreateModalOpen" />
    <EditCategoryModal v-model:open="isEditModalOpen" :category="selectedCategory" />
    <DeleteCategoryModal v-model:open="isDeleteModalOpen" :category="selectedCategory" />
  </UContainer>
</template>
