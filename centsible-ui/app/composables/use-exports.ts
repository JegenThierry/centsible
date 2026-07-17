import {computed, ref, watch} from 'vue';
import adze from 'adze'
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

  // Wired straight to a table event, so there is no caller to catch a rejection: rethrowing here
  // just produced a second unhandled rejection on top of the toast.
  async function retrigger(jobId: string) {
    try {
      const updated = await service.retriggerExport(jobId);
      exports.value = exports.value.map((j) => (j.id === updated.id ? updated : j));
      toasts.success(t('exports.toasts.retriggeredTitle'), t('exports.toasts.retriggeredBody'));
    } catch (e) {
      adze.ns('exports').error('Failed to retrigger export', e);
      toasts.error(t('exports.toasts.retriggerFailedTitle'), t('exports.toasts.retriggerFailedBody'));
    }
  }

  /**
   * Unlike {@link retrigger}, this one keeps its rethrow: its only caller hands it to
   * ConfirmationModal as `deleteCallback`, which awaits it inside a try/catch — so the rejection is
   * handled, and it's what keeps the modal open on a failed delete instead of closing it as if the
   * export were gone.
   */
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

  // Nothing downstream catches this either (table event → here → axios), and a file cleaned up
  // server-side or a 500 made the click do nothing at all except log an unhandled rejection.
  async function download(job: ExportJob) {
    try {
      await service.downloadExport(job.id, job.pdfFilename ?? undefined);
    } catch (e) {
      adze.ns('exports').error('Failed to download export', e);
      toasts.error(t('exports.toasts.downloadFailedTitle'), t('exports.toasts.downloadFailedBody'));
    }
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
