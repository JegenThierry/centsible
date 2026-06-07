package beer.thierry.centsible.api.services.loans

import beer.thierry.centsible.api.model.loan.LoanDTO
import beer.thierry.centsible.api.model.loan.LoanForm
import beer.thierry.centsible.api.model.loan.LoanUpdateForm
import beer.thierry.centsible.api.model.loan.RepaymentDTO
import beer.thierry.centsible.api.model.loan.RepaymentForm
import beer.thierry.centsible.api.model.user.UserDTO
import java.math.BigDecimal
import java.util.UUID

interface ILoanService {
    fun fetchAllLoans(authenticatedUser: UserDTO): List<LoanDTO>
    fun fetchLoansByContact(authenticatedUser: UserDTO, contactId: UUID): List<LoanDTO>
    fun fetchLoanById(authenticatedUser: UserDTO, id: UUID): LoanDTO?
    fun createLoan(authenticatedUser: UserDTO, form: LoanForm): LoanDTO
    fun updateLoan(authenticatedUser: UserDTO, id: UUID, form: LoanUpdateForm): LoanDTO?
    fun deleteLoan(authenticatedUser: UserDTO, id: UUID): Boolean

    fun fetchRepayments(authenticatedUser: UserDTO, loanId: UUID): List<RepaymentDTO>
    fun recordRepayment(authenticatedUser: UserDTO, loanId: UUID, form: RepaymentForm): RepaymentDTO
    fun deleteRepayment(authenticatedUser: UserDTO, loanId: UUID, repaymentId: UUID): Boolean

    fun totalOutstanding(authenticatedUser: UserDTO): BigDecimal
}
