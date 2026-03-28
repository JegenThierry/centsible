package beer.thierry.budgetplannerrest.service.account

import beer.thierry.budgetplannerrest.model.budgetaccount.BudgetAccountDTO
import beer.thierry.budgetplannerrest.model.budgetaccount.CreateBudgetAccountRequest
import beer.thierry.budgetplannerrest.model.user.UserDTO
import beer.thierry.budgetplannerrest.repository.accounts.IBudgetAccountsRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class BudgetBudgetAccountService(private val accountRepository: IBudgetAccountsRepository) : IBudgetAccountService {

    override fun createAccount(
        createBudgetAccountRequest: CreateBudgetAccountRequest,
        authenticatedUser: UserDTO,
    ): BudgetAccountDTO {
        return accountRepository.createAccount(authenticatedUser, createBudgetAccountRequest)
    }

    override fun fetchAccounts(authenticatedUser: UserDTO): List<BudgetAccountDTO> {
        return accountRepository.fetchAllAccounts(authenticatedUser)
    }

    override fun fetchAccountById(id: String, authenticatedUser: UserDTO): BudgetAccountDTO {
        return accountRepository.fetchAccountById(UUID.fromString(id), authenticatedUser)
    }
}