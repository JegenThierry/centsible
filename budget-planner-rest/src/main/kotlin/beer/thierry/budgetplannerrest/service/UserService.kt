package beer.thierry.budgetplannerrest.service

import beer.thierry.budgetplannerrest.model.user.UserDTO
import beer.thierry.budgetplannerrest.repository.users.IUserRepository
import org.springframework.stereotype.Service

@Service
class UserService(private val userRepository: IUserRepository) : IUserService {
    override fun fetchAllUsers(): List<UserDTO> {
        return userRepository.findAllUsers()
    }

    override fun fetchUserByUsername(username: String): UserDTO {
        val user = userRepository.findUserByUsername(username)
            ?: throw IllegalArgumentException("User with username $username not found")
        return UserDTO(
            id = user.id,
            username = user.username,
            email = user.email,
            name = "${user.firstName} ${user.lastName}",
            image = "",
        )
    }
}