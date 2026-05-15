<script lang="ts" setup>
</script>

<template>
  <section class="pb-20 sm:pb-28">
    <UContainer>
      <UPageCard
        class="bg-gradient-to-br from-primary/10 via-primary/5 to-transparent border border-default"
        description="Spin up an account and start tracking in under a minute. Your data stays on your server."
        spotlight
        spotlight-color="primary"
        title="Ready to plan smarter?">
        <template #footer>
          <div class="flex flex-col sm:flex-row gap-3 sm:justify-end">
            <UButton color="primary"
                     icon="i-lucide-user-plus"
                     size="lg"
                     to="/auth"
                     trailing-icon="i-lucide-arrow-right">
              Create an account
            </UButton>
            <UButton color="neutral"
                     icon="i-lucide-log-in"
                     size="lg"
                     to="/auth"
                     variant="outline">
              I already have one
            </UButton>
          </div>
        </template>
      </UPageCard>
    </UContainer>
  </section>
</template>
