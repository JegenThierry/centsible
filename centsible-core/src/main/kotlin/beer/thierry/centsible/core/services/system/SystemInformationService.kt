package beer.thierry.centsible.core.services.system

import beer.thierry.centsible.api.model.system.SystemInformationDTO
import beer.thierry.centsible.api.repository.ISystemInformationRepository
import beer.thierry.centsible.api.services.system.ISystemInformationService
import org.springframework.stereotype.Service

@Service
class SystemInformationService(
    private val systemInformationRepository: ISystemInformationRepository
) : ISystemInformationService {

    override fun fetchSystemInformation(): SystemInformationDTO? =
        systemInformationRepository.fetchSystemInformation()
}
