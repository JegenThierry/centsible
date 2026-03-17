package beer.thierry.budgetplannerrest.service.account

import beer.thierry.budgetplannerrest.model.account.AccountDTO
import beer.thierry.budgetplannerrest.model.account.CreateAccountRequest
import beer.thierry.budgetplannerrest.model.user.User
import beer.thierry.budgetplannerrest.repository.accounts.IAccountsRepository
import org.springframework.stereotype.Service

@Service
class AccountService(private val accountRepository: IAccountsRepository) : IAccountService {

    override fun createAccount(
        authenticatedUser: User,
        createAccountRequest: CreateAccountRequest
    ): AccountDTO {
        return accountRepository.createAccount(authenticatedUser, createAccountRequest)
    }

    override fun fetchAccounts(authenticatedUser: User): List<AccountDTO> {
        return accountRepository.fetchAllAccounts(authenticatedUser)
    }
}