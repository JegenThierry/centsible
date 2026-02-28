<script setup lang="ts">
import CreateAccountModal from "~/components/account/create-account-modal.vue";
import NoAccountAction from "~/components/dashboard/no-account-action.vue";

const isCreateAccountModalVisible = ref(false);

function onCreateAccount(): void {
  isCreateAccountModalVisible.value = true;
}
</script>

<template>
  <UContainer class="flex justify-center p-4 lg:p-10">
    <NoAccountAction @create-account="onCreateAccount" />
  </UContainer>

  <CreateAccountModal v-model="isCreateAccountModalVisible" />
</template>

<style scoped>

</style>