package beer.thierry.centsible.api.services.system

import beer.thierry.centsible.api.model.system.SystemInformationDTO

interface ISystemInformationService {
    fun fetchSystemInformation(): SystemInformationDTO?
}
