package beer.thierry.budgetplannerexport

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication(scanBasePackages = ["beer.thierry.budgetplanner", "beer.thierry.budgetplannerexport"])
@EnableScheduling
class BudgetPlannerExportApplication

fun main(args: Array<String>) {
    runApplication<BudgetPlannerExportApplication>(*args)
}
