package beer.thierry.centsiblerest.resources

import beer.thierry.centsible.api.model.reports.SafeToSpendDTO
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.services.reports.ISafeToSpendService
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RequestMapping("/api/reports/safe-to-spend")
@RestController
class SafeToSpendResource(
    private val safeToSpendService: ISafeToSpendService,
) {

    @GetMapping
    fun fetch(@AuthenticationPrincipal authenticatedUser: UserDTO): SafeToSpendDTO =
        safeToSpendService.fetchForCurrentMonth(authenticatedUser)
}
