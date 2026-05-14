package beer.thierry.budgetplanner.api.services.system

import beer.thierry.budgetplanner.api.model.system.SystemInformationDTO

interface ISystemInformationService {
    fun fetchSystemInformation(): SystemInformationDTO?
}
