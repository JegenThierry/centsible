package beer.thierry.budgetplannerrest.repository.categories

import beer.thierry.budgetplannerrest.model.category.CategoryDTO

interface ICategoriesRepository {
    fun fetchAllCategories() : List<CategoryDTO>
}