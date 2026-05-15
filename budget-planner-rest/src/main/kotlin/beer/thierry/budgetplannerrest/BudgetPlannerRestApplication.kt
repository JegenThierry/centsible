package beer.thierry.budgetplannerrest

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication(scanBasePackages = ["beer.thierry.budgetplanner", "beer.thierry.budgetplannerrest"])
@EnableScheduling
class BudgetPlannerRestApplication

fun main(args: Array<String>) {
    runApplication<BudgetPlannerRestApplication>(*args)
}
