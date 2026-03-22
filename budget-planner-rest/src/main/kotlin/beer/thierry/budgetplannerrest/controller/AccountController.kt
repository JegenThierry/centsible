package beer.thierry.budgetplannerrest.controller

import beer.thierry.budgetplannerrest.model.account.CreateAccountRequest
import beer.thierry.budgetplannerrest.service.account.IAccountService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RequestMapping("/api/accounts")
@RestController
class AccountController(private val accountService: IAccountService) {

    @PostMapping("")
    fun createAccount(createAccountRequest: CreateAccountRequest): ResponseEntity<String> {
//        accountService.createAccount(createAccountRequest)
        return ResponseEntity.ok("OK")
    }

}