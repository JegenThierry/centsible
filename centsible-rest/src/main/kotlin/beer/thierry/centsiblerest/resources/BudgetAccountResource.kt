package beer.thierry.centsiblerest.resources

import beer.thierry.centsible.api.model.budgetaccount.BudgetAccountDTO
import beer.thierry.centsible.api.model.budgetaccount.BudgetAccountSnapshotDTO
import beer.thierry.centsible.api.model.budgetaccount.CreateBudgetAccountRequest
import beer.thierry.centsible.api.model.budgetaccount.UpdateBudgetAccountRequest
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

    @PutMapping("/{id}")
    fun updateAccount(
        @PathVariable id: String,
        @Valid @RequestBody updateBudgetAccountRequest: UpdateBudgetAccountRequest,
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): ResponseEntity<BudgetAccountDTO> {
        val updated = budgetAccountService.updateAccount(id, updateBudgetAccountRequest, authenticatedUser)
        log.info("Updated budget account id={} userId={}", updated.id, authenticatedUser.id)
        return ResponseEntity.ok(updated)
    }

    @DeleteMapping("/{id}")
    fun deleteAccount(
        @PathVariable id: String,
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): ResponseEntity<Void> {
        budgetAccountService.deleteAccount(id, authenticatedUser)
        log.info("Deleted budget account id={} userId={}", id, authenticatedUser.id)
        return ResponseEntity.noContent().build()
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
