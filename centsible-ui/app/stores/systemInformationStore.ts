import {defineStore} from "pinia";
import type {SystemInformation} from "~/models/system/system-information";
import {useSystemInformationService} from "~/services/system/system-information-service";
import {useApiErrors} from "~/composables/use-api-errors";

export const useSystemInformationStore = defineStore('systemInformationStore', () => {
  const api = useApi();
  const systemInformationService = useSystemInformationService(api);

  const systemInformation = ref<SystemInformation>();
  const pending = ref(false);

  async function fetchSystemInformation() {
    pending.value = true;
    try {
      systemInformation.value = await systemInformationService.fetchSystemInformation();
    } catch (error) {
      useApiErrors().toastError(error, "Failed to load system information", "Could not load system information");
    } finally {
      pending.value = false;
    }
  }

  return {
    systemInformation,
    pending,
    fetchSystemInformation,
  };
});
