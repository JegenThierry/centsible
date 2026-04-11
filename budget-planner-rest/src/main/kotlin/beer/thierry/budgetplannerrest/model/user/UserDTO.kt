package beer.thierry.budgetplannerrest.model.user

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import java.util.UUID

data class UserDTO(
    val id: UUID,
    @get:JvmName("getUsernameValue")
    val username: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val name: String,
    val profilePicture: String?,
    private val authorities: Collection<GrantedAuthority> = listOf(SimpleGrantedAuthority("ROLE_USER"))

) : UserDetails {
    override fun getAuthorities() = authorities
    override fun getPassword() = null
    override fun getUsername() = username
    override fun isAccountNonExpired() = true
    override fun isAccountNonLocked() = true
    override fun isCredentialsNonExpired() = true
    override fun isEnabled() = true
}