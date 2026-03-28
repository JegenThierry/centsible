package beer.thierry.budgetplannerrest.controller

import beer.thierry.budgetplannerrest.model.account.AccountDTO
import beer.thierry.budgetplannerrest.model.account.CreateAccountRequest
import beer.thierry.budgetplannerrest.model.user.UserDTO
import beer.thierry.budgetplannerrest.service.account.IAccountService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RequestMapping("/api/accounts")
@RestController
class AccountController(private val accountService: IAccountService) {

    @PostMapping("")
    fun createAccount(
        createAccountRequest: CreateAccountRequest,
        @AuthenticationPrincipal authenticatedUser: UserDTO
    ): ResponseEntity<AccountDTO> {
        val response = accountService.createAccount(authenticatedUser, createAccountRequest)
        return ResponseEntity.ok(response)
    }

}