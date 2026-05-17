import type {AxiosInstance} from "axios";
import type {NetWorthPoint} from "~/models/reports/net-worth-point";
import {validateRequest} from "~/composables/use-api";

export function useReportsService(api: AxiosInstance) {
  async function fetchNetWorth(startDate: string, endDate: string): Promise<NetWorthPoint[]> {
    const response = await api.get<NetWorthPoint[]>('/reports/net-worth', {
      params: {startDate, endDate}
    });
    return validateRequest<NetWorthPoint[]>(response);
  }

  return {
    fetchNetWorth,
  }
}
