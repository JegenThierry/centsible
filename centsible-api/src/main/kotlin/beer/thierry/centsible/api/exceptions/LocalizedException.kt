package beer.thierry.centsible.api.exceptions

import org.springframework.context.MessageSourceResolvable

/**
 * Exception carrying a message-bundle key (plus optional args) instead of a localized string.
 * The REST layer's `GlobalExceptionHandler` resolves the key against the active request locale
 * so services don't have to know about i18n, locale resolution, or Spring's MessageSource.
 *
 * Use [BadRequest] (400) or [Conflict] (409) etc. via factory methods rather than this base.
 */
sealed class LocalizedException(
    val messageKey: String,
    val args: Array<out Any>,
    val httpStatus: Int,
) : RuntimeException(messageKey), MessageSourceResolvable {
    override fun getCodes(): Array<String> = arrayOf(messageKey)
    override fun getArguments(): Array<out Any> = args
    override fun getDefaultMessage(): String? = messageKey

    class BadRequest(key: String, vararg args: Any) : LocalizedException(key, args, 400)
    class Unauthorized(key: String, vararg args: Any) : LocalizedException(key, args, 401)
    class Forbidden(key: String, vararg args: Any) : LocalizedException(key, args, 403)
    class NotFound(key: String, vararg args: Any) : LocalizedException(key, args, 404)
    class Conflict(key: String, vararg args: Any) : LocalizedException(key, args, 409)
    class InternalError(key: String, vararg args: Any) : LocalizedException(key, args, 500)
}
