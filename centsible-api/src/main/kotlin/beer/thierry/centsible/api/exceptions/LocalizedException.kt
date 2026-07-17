package beer.thierry.centsible.api.exceptions

import org.springframework.context.MessageSourceResolvable

sealed class LocalizedException(
    val messageKey: String,
    val args: Array<out Any>,
    val httpStatus: Int,
) : RuntimeException(messageKey), MessageSourceResolvable {
    override fun getCodes(): Array<String> = arrayOf(messageKey)
    override fun getArguments(): Array<out Any> = args
    override fun getDefaultMessage(): String = messageKey

    class BadRequest(key: String, vararg args: Any) : LocalizedException(key, args, 400)
    class Unauthorized(key: String, vararg args: Any) : LocalizedException(key, args, 401)
    class Forbidden(key: String, vararg args: Any) : LocalizedException(key, args, 403)
    class NotFound(key: String, vararg args: Any) : LocalizedException(key, args, 404)
    class Conflict(key: String, vararg args: Any) : LocalizedException(key, args, 409)
    class InternalError(key: String, vararg args: Any) : LocalizedException(key, args, 500)
}
