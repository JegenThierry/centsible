package beer.thierry.budgetplannerrest

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class BudgetPlannerRestApplication

fun main(args: Array<String>) {
    runApplication<BudgetPlannerRestApplication>(*args)
}
