package beer.thierry.centsible.core.services.email

import beer.thierry.centsible.api.model.user.User
import beer.thierry.centsible.api.services.email.IEmailService
import beer.thierry.centsible.api.services.email.IPasswordResetEmailService
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.MessageSource
import org.springframework.stereotype.Service
import org.springframework.web.util.HtmlUtils
import java.util.Locale

@Service
class PasswordResetEmailService(
    private val emailService: IEmailService,
    private val messageSource: MessageSource,
    // Must be the user-facing origin (where the UI is served), not the REST host.
    @Value("\${app.base-url:http://localhost:3000}") private val baseUrl: String,
) : IPasswordResetEmailService {
    override fun sendPasswordResetEmail(user: User, token: String, locale: Locale) {
        val url = HtmlUtils.htmlEscape("$baseUrl/auth/reset-password?token=$token")
        val greetingName = user.firstName.takeIf { it.isNotBlank() }
            ?.let { HtmlUtils.htmlEscape(it) }
            ?: t("email.passwordReset.greeting.fallbackName", locale)

        val copy = EmailCopy(
            subject = t("email.passwordReset.subject", locale),
            preheader = t("email.passwordReset.preheader", locale),
            eyebrow = t("email.passwordReset.eyebrow", locale),
            heading = t("email.passwordReset.heading", locale, greetingName),
            body = t("email.passwordReset.body", locale),
            cta = t("email.passwordReset.cta", locale),
            fallbackPrompt = t("email.passwordReset.fallbackPrompt", locale),
            expiryNotice = t("email.passwordReset.expiry", locale),
            footer = t("email.passwordReset.footer", locale),
            htmlLang = locale.language.lowercase().ifBlank { "en" },
        )

        emailService.sendEmail(user, copy.subject, buildEmailHtml(copy, url))
    }

    private fun t(key: String, locale: Locale, vararg args: Any): String =
        messageSource.getMessage(key, args, key, locale) ?: key

    private data class EmailCopy(
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

    private fun buildEmailHtml(copy: EmailCopy, url: String): String = """
        <!DOCTYPE html>
        <html lang="${copy.htmlLang}">
        <head>
          <meta charset="UTF-8"/>
          <meta name="viewport" content="width=device-width,initial-scale=1"/>
          <meta name="color-scheme" content="light only"/>
          <meta name="supported-color-schemes" content="light"/>
          <title>${copy.subject}</title>
        </head>
        <body style="margin:0;padding:0;background-color:#fff1f6;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Oxygen,Ubuntu,Cantarell,'Helvetica Neue',Arial,sans-serif;color:#0f172a;-webkit-font-smoothing:antialiased;">
          <div style="display:none;max-height:0;overflow:hidden;font-size:1px;line-height:1px;color:#fff1f6;opacity:0;">
            ${copy.preheader}
          </div>

          <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="background-color:#fff1f6;padding:32px 16px;">
            <tr>
              <td align="center">
                <table role="presentation" width="560" cellpadding="0" cellspacing="0" border="0" style="max-width:560px;width:100%;">
                  <tr>
                    <td align="center" style="padding:8px 0 24px 0;">
                      <table role="presentation" cellpadding="0" cellspacing="0" border="0">
                        <tr>
                          <td style="background-color:#ee387e;border-radius:10px;padding:8px;line-height:0;" valign="middle">
                            <svg width="22" height="22" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
                              <g fill="none" stroke="#ffffff" stroke-linecap="round" stroke-linejoin="round" stroke-width="2.25">
                                <path d="M11 17h3v2a1 1 0 0 0 1 1h2a1 1 0 0 0 1-1v-3a3.16 3.16 0 0 0 2-2h1a1 1 0 0 0 1-1v-2a1 1 0 0 0-1-1h-1a5 5 0 0 0-2-4V3a4 4 0 0 0-3.2 1.6l-.3.4H11a6 6 0 0 0-6 6v1a5 5 0 0 0 2 4v3a1 1 0 0 0 1 1h2a1 1 0 0 0 1-1z"/>
                                <path d="M16 10h.01M2 8v1a2 2 0 0 0 2 2h1"/>
                              </g>
                            </svg>
                          </td>
                          <td style="padding-left:10px;font-size:18px;font-weight:600;letter-spacing:-0.2px;color:#0f172a;" valign="middle">
                            Centsible
                          </td>
                        </tr>
                      </table>
                    </td>
                  </tr>

                  <tr>
                    <td style="background-color:#ffffff;border-radius:16px;border:1px solid #ffe4ee;padding:40px 36px;box-shadow:0 1px 2px rgba(15,23,42,0.04);">
                      <p style="margin:0 0 8px 0;font-size:13px;font-weight:600;letter-spacing:1px;text-transform:uppercase;color:#b50f4d;">
                        ${copy.eyebrow}
                      </p>
                      <h1 style="margin:0 0 16px 0;font-size:26px;line-height:1.25;font-weight:700;letter-spacing:-0.4px;color:#0f172a;">
                        ${copy.heading}
                      </h1>
                      <p style="margin:0 0 28px 0;font-size:15px;line-height:1.6;color:#334155;">
                        ${copy.body}
                      </p>

                      <table role="presentation" cellpadding="0" cellspacing="0" border="0" style="margin:0 0 28px 0;">
                        <tr>
                          <td bgcolor="#ee387e" style="border-radius:10px;">
                            <a href="$url"
                               style="display:inline-block;padding:14px 28px;font-size:15px;font-weight:600;color:#ffffff;text-decoration:none;border-radius:10px;background-color:#ee387e;line-height:1;">
                              ${copy.cta}
                            </a>
                          </td>
                        </tr>
                      </table>

                      <p style="margin:0 0 8px 0;font-size:13px;line-height:1.5;color:#64748b;">
                        ${copy.fallbackPrompt}
                      </p>
                      <p style="margin:0;font-size:13px;line-height:1.5;word-break:break-all;">
                        <a href="$url" style="color:#d8195f;text-decoration:underline;">$url</a>
                      </p>

                      <hr style="border:none;border-top:1px solid #ffe4ee;margin:32px 0;"/>

                      <p style="margin:0;font-size:13px;line-height:1.6;color:#64748b;">
                        ${copy.expiryNotice}
                      </p>
                    </td>
                  </tr>

                  <tr>
                    <td align="center" style="padding:24px 16px 8px 16px;font-size:12px;line-height:1.5;color:#94a3b8;">
                      ${copy.footer}
                    </td>
                  </tr>
                </table>
              </td>
            </tr>
          </table>
        </body>
        </html>
    """.trimIndent()
}
