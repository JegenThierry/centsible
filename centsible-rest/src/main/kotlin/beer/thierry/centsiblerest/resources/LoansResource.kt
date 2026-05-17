package beer.thierry.centsiblerest.resources

import beer.thierry.centsible.api.model.loan.LoanDTO
import beer.thierry.centsible.api.model.loan.LoanForm
import beer.thierry.centsible.api.model.loan.RepaymentDTO
import beer.thierry.centsible.api.model.loan.RepaymentForm
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.services.loans.ILoanService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.util.UUID

@RequestMapping("/api/loans")
@RestController
class LoansResource(private val loanService: ILoanService) {

    @GetMapping
    fun list(
        @RequestParam(required = false) contactId: UUID?,
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): ResponseEntity<List<LoanDTO>> = ResponseEntity.ok(
        contactId?.let { loanService.fetchLoansByContact(authenticatedUser, it) }
            ?: loanService.fetchAllLoans(authenticatedUser)
    )

    @GetMapping("/outstanding")
    fun outstanding(@AuthenticationPrincipal authenticatedUser: UserDTO): ResponseEntity<Map<String, BigDecimal>> =
        ResponseEntity.ok(mapOf("outstanding" to loanService.totalOutstanding(authenticatedUser)))

    @GetMapping("/{id}")
    fun get(
        @PathVariable id: UUID,
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): ResponseEntity<LoanDTO> {
        val loan = loanService.fetchLoanById(authenticatedUser, id) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(loan)
    }

    @PostMapping
    fun create(
        @Valid @RequestBody form: LoanForm,
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): ResponseEntity<LoanDTO> =
        ResponseEntity.ok(loanService.createLoan(authenticatedUser, form))

    @DeleteMapping("/{id}")
    fun delete(
        @PathVariable id: UUID,
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): ResponseEntity<Void> =
        if (loanService.deleteLoan(authenticatedUser, id)) ResponseEntity.ok().build()
        else ResponseEntity.notFound().build()

    @GetMapping("/{id}/repayments")
    fun listRepayments(
        @PathVariable id: UUID,
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): ResponseEntity<List<RepaymentDTO>> =
        ResponseEntity.ok(loanService.fetchRepayments(authenticatedUser, id))

    @PostMapping("/{id}/repayments")
    fun recordRepayment(
        @PathVariable id: UUID,
        @Valid @RequestBody form: RepaymentForm,
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): ResponseEntity<RepaymentDTO> =
        ResponseEntity.ok(loanService.recordRepayment(authenticatedUser, id, form))

    @DeleteMapping("/{loanId}/repayments/{repaymentId}")
    fun deleteRepayment(
        @PathVariable loanId: UUID,
        @PathVariable repaymentId: UUID,
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): ResponseEntity<Void> =
        if (loanService.deleteRepayment(authenticatedUser, loanId, repaymentId)) ResponseEntity.ok().build()
        else ResponseEntity.notFound().build()
}
