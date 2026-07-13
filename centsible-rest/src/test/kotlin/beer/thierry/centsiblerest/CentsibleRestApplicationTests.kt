package beer.thierry.centsiblerest

import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

// Boots the full Spring context and therefore needs a reachable Postgres + real
// application.properties. Runs via `./gradlew integrationTest`, not the default `test` task.
@Tag("integration")
@SpringBootTest
class CentsibleRestApplicationTests {

    @Test
    fun contextLoads() {
    }

}
