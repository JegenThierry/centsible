package beer.thierry.budgetplannerrest

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["beer.thierry.budgetplanner", "beer.thierry.budgetplannerrest"])
class BudgetPlannerRestApplication

fun main(args: Array<String>) {
    runApplication<BudgetPlannerRestApplication>(*args)
}
