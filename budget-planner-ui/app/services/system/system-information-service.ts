import type {AxiosInstance} from "axios";
import {validateRequest} from "~/composables/use-api";
import type {SystemInformation} from "~/models/system/system-information";

export function useSystemInformationService(api: AxiosInstance) {
  async function fetchSystemInformation(): Promise<SystemInformation> {
    const response = await api.get<SystemInformation>('/system');
    return validateRequest<SystemInformation>(response);
  }

  return {
    fetchSystemInformation,
  };
}
