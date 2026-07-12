import {computed, ref, watch} from 'vue';
import {useApi} from "~/composables/use-api";
import {useExportService} from "~/services/export/export-service";
import {useToasts} from "~/services/toasts/toast-service";
import type {CreateExportRequest, ExportJob} from "~/models/export/export-job";
import {fingerprint} from "~/utils/fingerprint";

const POLL_INTERVAL_MS = 3000;

const jobFingerprint = (jobs: ExportJob[]) =>
  fingerprint(jobs, (j) => `${j.id}|${j.status}|${j.modifiedAt}`);

export function useExports() {
  const api = useApi();
  const service = useExportService(api);
  const toasts = useToasts();
  const {t} = useI18n();

  const exports = ref<ExportJob[]>([]);
  const loading = ref(false);
  const submitting = ref(false);
  const error = ref<string | null>(null);

  const hasUnsettledJobs = computed(() =>
    exports.value.some((j) => j.status === 'PENDING' || j.status === 'IN_PROGRESS')
  );

  async function refresh() {
    loading.value = true;
    error.value = null;
    try {
      const next = await service.listExports();
      if (jobFingerprint(next) !== jobFingerprint(exports.value)) {
        exports.value = next;
      }
    } catch (e: unknown) {
      error.value = (e as Error)?.message ?? 'Failed to load exports';
    } finally {
      loading.value = false;
    }
  }

  const {pause, resume} = useIntervalFn(async () => {
    if (!hasUnsettledJobs.value) {
      pause();
      return;
    }
    await refresh();
  }, POLL_INTERVAL_MS, {immediate: false});

  function startPolling() {
    if (hasUnsettledJobs.value) resume();
  }

  function stopPolling() {
    pause();
  }

  watch(hasUnsettledJobs, (unsettled) => {
    if (unsettled) resume();
    else pause();
  });

  async function create(payload: CreateExportRequest): Promise<ExportJob | null> {
    submitting.value = true;
    error.value = null;
    try {
      const job = await service.createExport(payload);
      exports.value = [job, ...exports.value];
      return job;
    } catch (e: unknown) {
      error.value = (e as Error)?.message ?? 'Failed to create export';
      return null;
    } finally {
      submitting.value = false;
    }
  }

  async function retrigger(jobId: string) {
    try {
      const updated = await service.retriggerExport(jobId);
      exports.value = exports.value.map((j) => (j.id === updated.id ? updated : j));
      toasts.success(t('exports.toasts.retriggeredTitle'), t('exports.toasts.retriggeredBody'));
    } catch (e) {
      toasts.error(t('exports.toasts.retriggerFailedTitle'), t('exports.toasts.retriggerFailedBody'));
      throw e;
    }
  }

  async function remove(jobId: string) {
    try {
      await service.deleteExport(jobId);
      exports.value = exports.value.filter((j) => j.id !== jobId);
      toasts.success(t('exports.toasts.deletedTitle'), t('exports.toasts.deletedBody'));
    } catch (e) {
      toasts.error(t('exports.toasts.deleteFailedTitle'), t('exports.toasts.deleteFailedBody'));
      throw e;
    }
  }

  async function download(job: ExportJob) {
    await service.downloadExport(job.id, job.pdfFilename ?? undefined);
  }

  return {
    exports,
    loading,
    submitting,
    error,
    hasUnsettledJobs,
    refresh,
    startPolling,
    stopPolling,
    create,
    retrigger,
    remove,
    download,
  };
}
