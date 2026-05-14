package beer.thierry.budgetplanner.api.repository

import beer.thierry.budgetplanner.api.model.loan.RepaymentDTO
import beer.thierry.budgetplanner.api.model.loan.RepaymentForm
import beer.thierry.budgetplanner.api.model.user.UserDTO
import java.math.BigDecimal
import java.util.UUID

interface ILoanRepaymentsRepository {
    fun fetchRepayments(authenticatedUser: UserDTO, loanId: UUID): List<RepaymentDTO>
    fun createRepayment(authenticatedUser: UserDTO, loanId: UUID, form: RepaymentForm): RepaymentDTO
    fun deleteRepayment(authenticatedUser: UserDTO, loanId: UUID, repaymentId: UUID): Boolean
    fun totalRepaidForLoan(loanId: UUID): BigDecimal
}
