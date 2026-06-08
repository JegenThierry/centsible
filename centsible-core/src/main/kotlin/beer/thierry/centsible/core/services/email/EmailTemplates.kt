package beer.thierry.centsible.core.services.email

import io.pebbletemplates.pebble.PebbleEngine
import io.pebbletemplates.pebble.loader.ClasspathLoader
import io.pebbletemplates.pebble.template.PebbleTemplate
import org.springframework.context.MessageSource
import org.springframework.stereotype.Component
import org.springframework.web.util.HtmlUtils
import java.io.StringWriter
import java.util.Locale

/**
 * Shared layout + copy plumbing for transactional emails. Each callsite supplies a CTA URL and
 * a message-bundle key prefix (see [EmailTemplateRenderer.buildCopy]); the layout and styling
 * are deliberately identical so branding stays consistent.
 */
data class EmailCopy(
    val subject: String,
    val preheader: String,
    val eyebrow: String,
    val heading: String,
    val body: String,
    val cta: String,
    val fallbackPrompt: String,
    val expiryNotice: String,
    val footer: String,
    val htmlLang: String,
)

@Component
class EmailTemplateRenderer(private val messageSource: MessageSource) {

    private val emailTemplate: PebbleTemplate = PebbleEngine.Builder()
        .loader(ClasspathLoader().apply { prefix = "templates/emails/" })
        .strictVariables(false)
        .autoEscaping(true)
        .build()
        .getTemplate("transactional-email.peb")

    fun resolveMessage(key: String, locale: Locale, vararg args: Any): String =
        messageSource.getMessage(key, args, key, locale) ?: key

    /** HTML-escaped [rawName], or the localized [fallbackKey] message when it is blank or null. */
    fun renderGreetingName(rawName: String?, fallbackKey: String, locale: Locale): String =
        rawName?.takeIf { it.isNotBlank() }
            ?.let { HtmlUtils.htmlEscape(it) }
            ?: resolveMessage(fallbackKey, locale)

    /**
     * Builds an [EmailCopy] from a message-bundle key prefix. Each callsite owns a flat namespace
     * of keys ("$keyPrefix.subject", ".preheader", ".heading", ...) so the bundle layout mirrors
     * the [EmailCopy] shape. The heading is the only string that takes a placeholder (the greeting
     * name); everything else is plain.
     */
    fun buildCopy(keyPrefix: String, locale: Locale, greetingName: String): EmailCopy = EmailCopy(
        subject = resolveMessage("$keyPrefix.subject", locale),
        preheader = resolveMessage("$keyPrefix.preheader", locale),
        eyebrow = resolveMessage("$keyPrefix.eyebrow", locale),
        heading = resolveMessage("$keyPrefix.heading", locale, greetingName),
        body = resolveMessage("$keyPrefix.body", locale),
        cta = resolveMessage("$keyPrefix.cta", locale),
        fallbackPrompt = resolveMessage("$keyPrefix.fallbackPrompt", locale),
        expiryNotice = resolveMessage("$keyPrefix.expiry", locale),
        footer = resolveMessage("$keyPrefix.footer", locale),
        htmlLang = locale.language.lowercase().ifBlank { "en" },
    )

    fun renderHtml(copy: EmailCopy, url: String): String {
        val context = mapOf<String, Any?>(
            "htmlLang" to copy.htmlLang,
            "subject" to copy.subject,
            "preheader" to copy.preheader,
            "eyebrow" to copy.eyebrow,
            "heading" to copy.heading,
            "body" to copy.body,
            "cta" to copy.cta,
            "fallbackPrompt" to copy.fallbackPrompt,
            "expiryNotice" to copy.expiryNotice,
            "footer" to copy.footer,
            "url" to url,
        )
        val writer = StringWriter()
        emailTemplate.evaluate(writer, context)
        return writer.toString()
    }
}
