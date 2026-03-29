package beer.thierry.budgetplannerrest.service.users

import beer.thierry.budgetplannerrest.model.user.UserDTO

interface IUserService {
    fun fetchAllUsers(): List<UserDTO>
    fun fetchUserByUsername(username: String): UserDTO
}
