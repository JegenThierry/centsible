import {ref} from 'vue';
import {useApi} from "~/composables/use-api";
import {useExportService} from "~/services/export/export-service";
import {useToasts} from "~/services/toasts/toast-service";
import type {ExportSchedule, ExportScheduleForm, ExportScheduleUpdateForm} from "~/models/export/export-schedule";

/** State + actions for recurring export schedules (mirrors {@link useExports}, no polling needed). */
export function useExportSchedules() {
  const api = useApi();
  const service = useExportService(api);
  const toasts = useToasts();
  const {t} = useI18n();

  const schedules = ref<ExportSchedule[]>([]);
  const loading = ref(false);
  const submitting = ref(false);

  async function refresh() {
    loading.value = true;
    try {
      schedules.value = await service.listSchedules();
    } catch (e) {
      toasts.error(t('exports.schedules.toasts.loadFailedTitle'), t('exports.schedules.toasts.loadFailedBody'));
    } finally {
      loading.value = false;
    }
  }

  async function create(form: ExportScheduleForm): Promise<boolean> {
    submitting.value = true;
    try {
      await service.createSchedule(form);
      toasts.success(t('exports.schedules.toasts.createdTitle'), t('exports.schedules.toasts.createdBody'));
      await refresh();
      return true;
    } catch (e) {
      toasts.error(t('exports.schedules.toasts.errorTitle'), t('exports.schedules.toasts.errorBody'));
      return false;
    } finally {
      submitting.value = false;
    }
  }

  async function update(id: string, form: ExportScheduleUpdateForm): Promise<boolean> {
    submitting.value = true;
    try {
      await service.updateSchedule(id, form);
      toasts.success(t('exports.schedules.toasts.updatedTitle'), t('exports.schedules.toasts.updatedBody'));
      await refresh();
      return true;
    } catch (e) {
      toasts.error(t('exports.schedules.toasts.errorTitle'), t('exports.schedules.toasts.errorBody'));
      return false;
    } finally {
      submitting.value = false;
    }
  }

  async function toggleActive(schedule: ExportSchedule) {
    await update(schedule.id, {
      title: schedule.title,
      format: schedule.format,
      frequency: schedule.frequency,
      active: !schedule.active,
    });
  }

  async function remove(id: string) {
    try {
      await service.deleteSchedule(id);
      toasts.success(t('exports.schedules.toasts.deletedTitle'), t('exports.schedules.toasts.deletedBody'));
      await refresh();
    } catch (e) {
      toasts.error(t('exports.schedules.toasts.errorTitle'), t('exports.schedules.toasts.errorBody'));
    }
  }

  return {schedules, loading, submitting, refresh, create, update, toggleActive, remove};
}
