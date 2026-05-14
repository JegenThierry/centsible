package beer.thierry.budgetplanner.core.services.system

import beer.thierry.budgetplanner.api.model.system.SystemInformationDTO
import beer.thierry.budgetplanner.api.repository.ISystemInformationRepository
import beer.thierry.budgetplanner.api.services.system.ISystemInformationService
import org.springframework.stereotype.Service

@Service
class SystemInformationService(
    private val systemInformationRepository: ISystemInformationRepository
) : ISystemInformationService {

    override fun fetchSystemInformation(): SystemInformationDTO? =
        systemInformationRepository.fetchSystemInformation()
}
