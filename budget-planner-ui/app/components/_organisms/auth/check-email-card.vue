<script lang="ts" setup>
const props = defineProps<{
  email?: string;
}>();

const description = computed(() =>
  props.email
    ? `We sent a confirmation link to ${props.email}. Click the link in that email to activate your account.`
    : 'We sent you a confirmation link. Click the link in that email to activate your account.'
);
</script>

<template>
  <UContainer class="py-12">
    <UPageCard
      :description="description"
      :ui="{ leadingIcon: 'text-primary' }"
      class="max-w-xl mx-auto"
      icon="i-lucide-mail-check"
      spotlight
      spotlight-color="primary"
      title="Check your inbox">
      <p class="text-sm text-(--ui-text-muted)">
        The link expires in 24 hours. If you don't see the email after a minute or two, check your spam folder.
      </p>

      <template #footer>
        <div class="flex flex-col sm:flex-row gap-3 sm:justify-end">
          <UButton color="neutral"
                   icon="i-lucide-home"
                   size="lg"
                   to="/"
                   variant="outline">
            Back to home
          </UButton>
          <UButton color="primary"
                   icon="i-lucide-log-in"
                   size="lg"
                   to="/auth"
                   trailing-icon="i-lucide-arrow-right">
            Back to sign in
          </UButton>
        </div>
      </template>
    </UPageCard>
  </UContainer>
</template>
