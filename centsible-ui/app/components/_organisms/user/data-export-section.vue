<script lang="ts" setup>
import ExportButton from "~/components/_molecules/exports/export-button.vue";
import AppButton from "~/components/_atoms/ui/app-button.vue";
import {todayIsoDate} from "~/utils/date";

const {t} = useI18n();
</script>

<template>
  <div>
    <h3 class="text-lg font-semibold">{{ t('profile.dataExport.title') }}</h3>
    <p class="text-sm text-muted mt-1">{{ t('profile.dataExport.description') }}</p>

    <div class="mt-4 flex flex-wrap items-center gap-3">
      <ExportButton
        type="TRANSACTIONS"
        :default-title="t('profile.dataExport.defaultTitle', {date: todayIsoDate()})"
        :params-builder="() => ({ kind: 'TRANSACTIONS' })"
        :label="t('profile.dataExport.trigger')"/>
      <AppButton variant="ghost" color="neutral" icon="i-lucide-arrow-right" to="/exports">
        {{ t('profile.dataExport.viewAll') }}
      </AppButton>
    </div>
  </div>
</template>
