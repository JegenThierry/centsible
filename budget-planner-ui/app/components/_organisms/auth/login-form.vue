<script lang="ts" setup>
import {useApi} from "~/composables/use-api";
import {useAuthService} from "~/services/auth/auth-service";
import {useToasts} from "~/services/toasts/toast-service";
import type {AxiosInstance} from "axios";
import type {AuthResponse} from "~/models/auth/auth-response";

const api = useApi();
const authStore = useAuthStore();
const userStore = useUserStore();
const {success, error} = useToasts();

const state = reactive({
  username: '',
  password: '',
})
const loading = ref<boolean>(false);

function onSubmit() {
  loading.value = true;
  useAuthService(api as AxiosInstance)
    .login({username: state.username, password: state.password})
    .then((res: AuthResponse) => {
      authStore.setToken(res.token);
      userStore.fetchMyself();
      navigateTo('/accounts');
      success(
        'Login successfully',
        'You have been logged in, redirecting to the dashboard.'
      );
    })
    .catch((err) => {
      console.error(err);
      error(
        'Login failed',
        'Username or password are incorrect.'
      );
    })
    .finally(() => loading.value = false);
}
</script>

<template>
  <UForm :state="state" class="space-y-6 pt-4 flex flex-col" @submit="onSubmit">
    <UFormField autofocus label="Username" name="username">
      <UInput v-model="state.username" class="w-full"/>
    </UFormField>

    <UFormField label="Password" name="password">
      <UInput v-model="state.password" class="w-full" type="password"/>
    </UFormField>

    <UButton :loading="loading" class="ml-auto" type="submit">
      Submit
    </UButton>
  </UForm>
</template>
