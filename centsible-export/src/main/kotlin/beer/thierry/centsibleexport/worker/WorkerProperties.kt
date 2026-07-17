package beer.thierry.centsibleexport.worker

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration

@Configuration
class WorkerProperties(
    @param:Value("\${export.worker.id}") val id: String,
    @param:Value("\${export.worker.lease-timeout-seconds:300}") val leaseTimeoutSeconds: Long,
    @param:Value("\${export.worker.max-attempts:3}") val maxAttempts: Int,
)
