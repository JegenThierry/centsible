import axios from 'axios';
import adze from 'adze';
import {useBudgetAccountsStore} from "~/stores/budgetAccountsStore";

export default defineNuxtRouteMiddleware(async (to, _) => {
  const accountStore = useBudgetAccountsStore();
  const accountId = to.params.accountId as string;

  if (!accountId) {
    return;
  }

  try {
    await accountStore.loadActiveAccount(accountId);
  } catch (error) {
    const status = axios.isAxiosError(error) ? error.response?.status : undefined;

    if (status === 401 || status === 403) {
      accountStore.clearActiveAccount();
      return navigateTo('/auth');
    }

    if (status === 404) {
      accountStore.clearActiveAccount();
      return navigateTo('/accounts');
    }

    adze.ns('budget-accounts').error('Failed to load active account', error);
    throw createError({statusCode: 503, statusMessage: 'accountUnavailable', fatal: true});
  }
})
