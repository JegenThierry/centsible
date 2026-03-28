import {defineStore} from "pinia";
import type {BudgetAccount} from "~/models/budget-account/budget-account";
import {useBudgetAccountService} from "~/services/budget-account/budget-account-service";
import {useToasts} from "~/services/toasts/toast-service";

export const useBudgetAccountsStore = defineStore(
    'budgetAccountsStore',
    () => {
        const api = useApi();
        const toasts = useToasts();
        const accountService = useBudgetAccountService(api);

        const activeAccount = ref<BudgetAccount>();
        const availableAccounts = ref<BudgetAccount[]>([]);

        async function updateAvailableAccounts() {
            try {
                availableAccounts.value = await accountService.fetchAccounts();
            } catch (error) {
                toasts.error("Failed to update accounts", "Accounts could not be updated")
                console.error(error);
            }
        }

        return {
            activeAccount,
            availableAccounts,
            updateAvailableAccounts,
        }
    }
);
