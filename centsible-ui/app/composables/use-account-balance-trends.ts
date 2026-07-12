import adze from 'adze'
import {useReportsService} from "~/services/reports/reports-service";
import {daysAgoIsoDate} from "~/utils/date";

/** Loads each account's balance as of [days] ago, keyed by account id, for trend comparison against current balances. */
export function useAccountBalanceTrends(days = 30) {
  const reportsService = useReportsService(useApi());

  const previousBalances = ref<Record<string, number>>({});
  const loaded = ref(false);

  async function refresh(): Promise<void> {
    try {
      const rows = await reportsService.fetchNetWorthBreakdown(daysAgoIsoDate(days));
      previousBalances.value = Object.fromEntries(rows.map(row => [row.accountId, Number(row.balance)]));
      loaded.value = true;
    } catch (error) {
      adze.ns('budget-accounts').error('Failed to load account balance trends', error);
    }
  }

  return {previousBalances, loaded, refresh};
}
