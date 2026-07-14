import type {AxiosInstance} from "axios";
import type {RegisterRequest} from "~/models/user/register-request";
import {validateRequest} from "~/composables/use-api";

/** Client for the first-run setup endpoints (public but self-disabling on the server). */
export function useSetupService(api: AxiosInstance) {
  /** True on a brand-new instance with no users yet. */
  async function status(): Promise<boolean> {
    const response = await api.get<{needsSetup: boolean}>('/setup');
    return validateRequest<{needsSetup: boolean}>(response).needsSetup;
  }

  /** Creates the first (admin) user; the server issues the session cookie on success. */
  async function create(request: RegisterRequest): Promise<void> {
    await api.post('/setup', request);
  }

  return {status, create};
}
