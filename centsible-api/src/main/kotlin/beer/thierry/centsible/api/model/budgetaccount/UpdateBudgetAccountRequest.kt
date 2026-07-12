package beer.thierry.centsible.api.model.budgetaccount

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class UpdateBudgetAccountRequest(
    @field:NotBlank(message = "{validation.account.name.required}")
    @field:Size(max = 100, message = "{validation.account.name.tooLong}")
    var name: String = "",

    var type: AccountType = AccountType.CHECKING,
)
