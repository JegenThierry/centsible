package beer.thierry.budgetplanner.api.repository

import beer.thierry.budgetplanner.api.model.system.SystemInformationDTO

interface ISystemInformationRepository {
    fun fetchSystemInformation(): SystemInformationDTO?
}
