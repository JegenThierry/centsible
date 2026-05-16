package beer.thierry.centsible.api.repository

import beer.thierry.centsible.api.model.loan.LoanDTO
import beer.thierry.centsible.api.model.loan.LoanForm
import beer.thierry.centsible.api.model.user.UserDTO
import java.math.BigDecimal
import java.util.UUID

interface ILoansRepository {
    fun fetchAllLoans(authenticatedUser: UserDTO): List<LoanDTO>
    fun fetchLoansByContact(authenticatedUser: UserDTO, contactId: UUID): List<LoanDTO>
    fun fetchLoanById(authenticatedUser: UserDTO, id: UUID): LoanDTO?
    fun createLoan(authenticatedUser: UserDTO, contactId: UUID, form: LoanForm): LoanDTO
    fun deleteLoan(authenticatedUser: UserDTO, id: UUID): Boolean
    fun totalOutstanding(authenticatedUser: UserDTO): BigDecimal
}
