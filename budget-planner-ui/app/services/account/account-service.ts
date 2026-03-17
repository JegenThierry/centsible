import type {AxiosInstance} from "axios";
import type {CreateAccountForm, UpdateAccountForm, Account} from "~/models/account/account";
import {validateRequest} from "~/composables/use-api";

export function useAccountService(api: AxiosInstance) {
    async function fetchAccounts(): Promise<Account[]> {
        return [];
    }

    async function fetchAccount(id: string): Promise<void> {
    }

    async function createAccount(createAccountForm: CreateAccountForm): Promise<Account> {
        const response = await api.post<Account>('/accounts', createAccountForm);
        return validateRequest<Account>(response);
    }

    async function updateAccount(createAccountForm: UpdateAccountForm): Promise<void> {
    }

    async function deleteAccount(): Promise<void> {
    }

    return {
        fetchAccounts,
        fetchAccount,
        createAccount,
        updateAccount,
        deleteAccount,
    }
}
