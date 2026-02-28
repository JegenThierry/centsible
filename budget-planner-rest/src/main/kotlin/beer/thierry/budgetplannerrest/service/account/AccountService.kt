package beer.thierry.budgetplannerrest.service.account

import beer.thierry.budgetplannerrest.model.account.CreateAccountRequest
import beer.thierry.budgetplannerrest.repository.users.IUserRepository
import org.springframework.stereotype.Service

@Service
class AccountService(private val userRepository: IUserRepository) : IAccountService {
    override fun createAccount(createAccountRequest: CreateAccountRequest) {
        TODO("Not yet implemented")
    }
}