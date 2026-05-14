import {computed, onUnmounted, ref, watch} from 'vue';
import {useApi} from "~/composables/use-api";
import {useExportService} from "~/services/export/export-service";
import type {CreateExportRequest, ExportJob} from "~/models/export/export-job";

const POLL_INTERVAL_MS = 3000;

function fingerprint(jobs: ExportJob[]): string {
  return jobs.map((j) => `${j.id}|${j.status}|${j.modifiedAt}`).join(',');
}

export function useExports() {
  const api = useApi();
  const service = useExportService(api);

  const exports = ref<ExportJob[]>([]);
  const loading = ref(false);
  const submitting = ref(false);
  const error = ref<string | null>(null);

  const hasUnsettledJobs = computed(() =>
    exports.value.some((j) => j.status === 'PENDING' || j.status === 'IN_PROGRESS')
  );

  let pollTimer: ReturnType<typeof setInterval> | null = null;
  let pollingRequested = false;

  async function refresh() {
    loading.value = true;
    error.value = null;
    try {
      const next = await service.listExports();
      if (fingerprint(next) !== fingerprint(exports.value)) {
        exports.value = next;
      }
    } catch (e: unknown) {
      error.value = (e as Error)?.message ?? 'Failed to load exports';
    } finally {
      loading.value = false;
    }
  }

  function startPolling() {
    pollingRequested = true;
    ensureTimer();
  }

  function stopPolling() {
    pollingRequested = false;
    clearTimer();
  }

  function ensureTimer() {
    if (pollTimer || !pollingRequested) return;
    pollTimer = setInterval(async () => {
      if (!hasUnsettledJobs.value) {
        clearTimer();
        return;
      }
      await refresh();
    }, POLL_INTERVAL_MS);
  }

  function clearTimer() {
    if (pollTimer) {
      clearInterval(pollTimer);
      pollTimer = null;
    }
  }

  watch(hasUnsettledJobs, (unsettled) => {
    if (unsettled) ensureTimer();
    else clearTimer();
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
    const updated = await service.retriggerExport(jobId);
    exports.value = exports.value.map((j) => (j.id === updated.id ? updated : j));
  }

  async function remove(jobId: string) {
    await service.deleteExport(jobId);
    exports.value = exports.value.filter((j) => j.id !== jobId);
  }

  async function download(job: ExportJob) {
    await service.downloadExport(job.id, job.pdfFilename ?? undefined);
  }

  onUnmounted(stopPolling);

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
