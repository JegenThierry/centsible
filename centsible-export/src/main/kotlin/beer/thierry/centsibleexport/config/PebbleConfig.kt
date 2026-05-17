package beer.thierry.centsibleexport.config

import beer.thierry.centsibleexport.render.pebble.MoneyExtension
import io.pebbletemplates.pebble.PebbleEngine
import io.pebbletemplates.pebble.loader.ClasspathLoader
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class PebbleConfig {
    @Bean
    fun pebbleEngine(): PebbleEngine =
        PebbleEngine.Builder()
            .loader(ClasspathLoader().apply { prefix = "templates/exports/" })
            .extension(MoneyExtension())
            .strictVariables(false)
            .autoEscaping(true)
            .build()
}
