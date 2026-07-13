package beer.thierry.centsible.core.services.admin

import beer.thierry.centsible.api.services.admin.IAdminAccessService
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class AdminAccessService(
    @Value("\${admin.enabled:false}") private val adminEnabled: Boolean,
    @Value("\${admin.username:}") private val adminUsername: String,
) : IAdminAccessService {

    override fun isEnabled(): Boolean = adminEnabled

    override fun isAdmin(username: String): Boolean =
        adminEnabled && adminUsername.isNotBlank() && username == adminUsername
}
