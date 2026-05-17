package beer.thierry.centsible.api.repository

import beer.thierry.centsible.api.model.system.SystemInformationDTO

interface ISystemInformationRepository {
    fun fetchSystemInformation(): SystemInformationDTO?
}
