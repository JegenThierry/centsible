package beer.thierry.centsiblerest.resources

import beer.thierry.centsible.api.model.loan.LoanDTO
import beer.thierry.centsible.api.model.loan.LoanForm
import beer.thierry.centsible.api.model.loan.LoanUpdateForm
import beer.thierry.centsible.api.model.loan.OutstandingTotalDTO
import beer.thierry.centsible.api.model.loan.RepaymentDTO
import beer.thierry.centsible.api.model.loan.RepaymentForm
import beer.thierry.centsible.api.model.loan.SplitToLoansForm
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.services.loans.ILoanService
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RequestMapping("/api/loans")
@RestController
class LoansResource(private val loanService: ILoanService) {

    private val log = LoggerFactory.getLogger(LoansResource::class.java)

    @GetMapping
    fun list(
        @RequestParam(required = false) contactId: UUID?,
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): List<LoanDTO> =
        contactId?.let { loanService.fetchLoansByContact(authenticatedUser, it) }
            ?: loanService.fetchAllLoans(authenticatedUser)

    @GetMapping("/outstanding")
    fun outstanding(@AuthenticationPrincipal authenticatedUser: UserDTO): OutstandingTotalDTO =
        loanService.totalOutstanding(authenticatedUser)

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
    ): LoanDTO {
        val created = loanService.createLoan(authenticatedUser, form)
        log.info("Created loan id={} userId={}", created.id, authenticatedUser.id)
        return created
    }

    /** Splits an existing expense into one tracking-only IOU per share (money owed back to the user). */
    @PostMapping("/from-transaction/{transactionId}")
    fun splitFromTransaction(
        @PathVariable transactionId: UUID,
        @Valid @RequestBody form: SplitToLoansForm,
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): List<LoanDTO> {
        val created = loanService.splitTransactionIntoLoans(authenticatedUser, transactionId, form)
        log.info("Split transaction id={} into {} IOUs userId={}", transactionId, created.size, authenticatedUser.id)
        return created
    }

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: UUID,
        @Valid @RequestBody form: LoanUpdateForm,
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): ResponseEntity<LoanDTO> {
        val updated = loanService.updateLoan(authenticatedUser, id, form) ?: return ResponseEntity.notFound().build()
        log.info("Updated loan id={} userId={}", id, authenticatedUser.id)
        return ResponseEntity.ok(updated)
    }

    @DeleteMapping("/{id}")
    fun delete(
        @PathVariable id: UUID,
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): ResponseEntity<Void> {
        val deleted = loanService.deleteLoan(authenticatedUser, id)
        if (deleted) log.info("Deleted loan id={} userId={}", id, authenticatedUser.id)
        return deleted.toDeleteResponse()
    }

    @GetMapping("/{id}/repayments")
    fun listRepayments(
        @PathVariable id: UUID,
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): List<RepaymentDTO> =
        loanService.fetchRepayments(authenticatedUser, id)

    @PostMapping("/{id}/repayments")
    fun recordRepayment(
        @PathVariable id: UUID,
        @Valid @RequestBody form: RepaymentForm,
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): RepaymentDTO {
        val created = loanService.recordRepayment(authenticatedUser, id, form)
        log.info("Recorded repayment id={} loanId={} userId={}", created.id, id, authenticatedUser.id)
        return created
    }

    @DeleteMapping("/{loanId}/repayments/{repaymentId}")
    fun deleteRepayment(
        @PathVariable loanId: UUID,
        @PathVariable repaymentId: UUID,
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): ResponseEntity<Void> {
        val deleted = loanService.deleteRepayment(authenticatedUser, loanId, repaymentId)
        if (deleted) log.info("Deleted repayment id={} loanId={} userId={}", repaymentId, loanId, authenticatedUser.id)
        return deleted.toDeleteResponse()
    }
}
