package beer.thierry.budgetplanner.jooq.repository

import beer.thierry.budgetplanner.api.model.system.SystemInformationDTO
import beer.thierry.budgetplanner.api.repository.ISystemInformationRepository
import beer.thierry.jooq.generated.tables.references.SYSTEM_INFORMATION
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class SystemInformationRepository(private val dsl: DSLContext) : ISystemInformationRepository {

    override fun fetchSystemInformation(): SystemInformationDTO? {
        return dsl.select(
            SYSTEM_INFORMATION.NAME,
            SYSTEM_INFORMATION.VERSION,
            SYSTEM_INFORMATION.DESCRIPTION,
            SYSTEM_INFORMATION.LICENSE,
            SYSTEM_INFORMATION.REPOSITORY,
            SYSTEM_INFORMATION.RELEASED_AT,
        )
            .from(SYSTEM_INFORMATION)
            .fetchOne { record ->
                SystemInformationDTO(
                    name = record[SYSTEM_INFORMATION.NAME] ?: "",
                    version = record[SYSTEM_INFORMATION.VERSION] ?: "",
                    description = record[SYSTEM_INFORMATION.DESCRIPTION],
                    license = record[SYSTEM_INFORMATION.LICENSE],
                    repository = record[SYSTEM_INFORMATION.REPOSITORY],
                    releasedAt = record[SYSTEM_INFORMATION.RELEASED_AT],
                )
            }
    }
}
