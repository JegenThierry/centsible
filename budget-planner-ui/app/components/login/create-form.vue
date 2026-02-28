<script setup lang="ts">
import {useApi} from "~/composables/use-api";
import {useAuthService} from "~/services/auth/auth-service";
import {useToasts} from "~/services/toasts/toast-service";
import type {AxiosInstance} from "axios";
import type {AuthResponse} from "~/models/auth/auth-response";
import BaseInput from "~/components/_atoms/inputs/base-input.vue";
import RegisterPasswordInput from "~/components/_organisms/inputs/register-password-input.vue";

const api = useApi();
const authStore = useAuthStore();
const {success, error} = useToasts();

const state = reactive({
  username: '',
  email: '',
  firstName: '',
  lastName: '',
  password: '',
  confirmPassword: '',
})
const loading = ref<boolean>(false);

function onSubmit() {
  loading.value = true;
  useAuthService(api as AxiosInstance)
      .register(state)
      .then((res: AuthResponse) => {
        authStore.setToken(res.token);
        navigateTo('/dashboard');
        success(
            'Registered successfully',
            'You have successfully registered, you will be redirected to the dashboard.'
        );
      })
      .catch((err) => {
        console.error(err);
        error(
            'Registration failed',
            'Please try again later.'
        );
      })
      .finally(() => loading.value = false);
}
</script>

<template>
  <UForm :state="state" class="space-y-6 pt-4 flex flex-col" @submit="onSubmit">
    <BaseInput v-model="state.username"
               label="Username"
               type="text"
               required />

    <BaseInput v-model="state.email"
               label="E-Mail"
               type="email"
               required />

    <BaseInput v-model="state.firstName"
               label="Firstname"
               type="text"
               required />

    <BaseInput v-model="state.lastName"
               label="Lastname"
               type="text"
               required />

    <RegisterPasswordInput v-model:password="state.password"
                           v-model:confirm-password="state.confirmPassword" />

    <UButton class="ml-auto" :loading="loading" type="submit">
      Register
    </UButton>
  </UForm>
</template>
