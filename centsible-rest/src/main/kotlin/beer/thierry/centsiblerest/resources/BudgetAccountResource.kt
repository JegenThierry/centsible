package beer.thierry.centsiblerest.resources

import beer.thierry.centsible.api.model.budgetaccount.BudgetAccountDTO
import beer.thierry.centsible.api.model.budgetaccount.BudgetAccountSnapshotDTO
import beer.thierry.centsible.api.model.budgetaccount.CreateBudgetAccountRequest
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.services.account.IBudgetAccountService
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RequestMapping("/api/budget-accounts")
@RestController
class BudgetAccountResource(private val budgetAccountService: IBudgetAccountService) {

    private val log = LoggerFactory.getLogger(BudgetAccountResource::class.java)

    @PostMapping("")
    fun createAccount(
        @Valid @RequestBody createBudgetAccountRequest: CreateBudgetAccountRequest,
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): ResponseEntity<BudgetAccountDTO> {
        val created = budgetAccountService.createAccount(createBudgetAccountRequest, authenticatedUser)
        log.info("Created budget account id={} userId={}", created.id, authenticatedUser.id)
        return ResponseEntity.ok(created)
    }

    @GetMapping("")
    fun fetchAccounts(@AuthenticationPrincipal authenticatedUser: UserDTO): ResponseEntity<List<BudgetAccountDTO>> =
        ResponseEntity.ok(budgetAccountService.fetchAccounts(authenticatedUser))

    @GetMapping("/{id}")
    fun findAccount(
        @PathVariable id: String,
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): ResponseEntity<BudgetAccountDTO> =
        ResponseEntity.ok(budgetAccountService.fetchAccountById(id, authenticatedUser))

    @GetMapping("/{id}/snapshots")
    fun fetchSnapshots(
        @PathVariable id: String,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate,
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): ResponseEntity<List<BudgetAccountSnapshotDTO>> =
        ResponseEntity.ok(budgetAccountService.fetchAccountSnapshots(id, startDate, endDate, authenticatedUser))
}
