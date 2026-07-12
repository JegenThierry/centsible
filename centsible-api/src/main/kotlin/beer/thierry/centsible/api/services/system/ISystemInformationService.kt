package beer.thierry.centsible.api.services.system

import beer.thierry.centsible.api.model.system.SystemInformationDTO

interface ISystemInformationService {
    /** Build/version metadata for the running instance, or null when none has been recorded. */
    fun fetchSystemInformation(): SystemInformationDTO?
}
