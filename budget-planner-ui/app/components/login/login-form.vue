<script setup lang="ts">
import {useApi} from "~/composables/use-api";
import {useAuthService} from "~/services/auth/auth-service";
import {useToasts} from "~/services/toasts/toast-service";
import type {AxiosInstance} from "axios";
import type {AuthResponse} from "~/models/auth/auth-response";

const api = useApi();
const authStore = useAuthStore();
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
        navigateTo('/dashboard');
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
    <UFormField label="Username" name="username">
      <UInput class="w-full" v-model="state.username"/>
    </UFormField>

    <UFormField label="Password" name="password">
      <UInput class="w-full" v-model="state.password" type="password"/>
    </UFormField>

    <UButton class="ml-auto" :loading="loading" type="submit">
      Submit
    </UButton>
  </UForm>
</template>
