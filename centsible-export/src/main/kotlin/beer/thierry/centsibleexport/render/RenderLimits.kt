package beer.thierry.centsibleexport.render

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration

@Configuration
class RenderLimits(
    @param:Value("\${export.render.max-transaction-rows:20000}") val maxTransactionRows: Int,
)
