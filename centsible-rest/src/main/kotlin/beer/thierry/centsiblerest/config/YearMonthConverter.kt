package beer.thierry.centsiblerest.config

import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.format.FormatterRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.time.YearMonth
import java.time.format.DateTimeParseException

/**
 * Binds `?month=YYYY-MM` request params to [YearMonth]. A malformed value surfaces as a
 * `MethodArgumentTypeMismatchException`, which the global exception handler maps to HTTP 400.
 */
@Configuration
class YearMonthConverter : WebMvcConfigurer, Converter<String, YearMonth> {

    override fun convert(source: String): YearMonth = try {
        YearMonth.parse(source)
    } catch (ex: DateTimeParseException) {
        throw IllegalArgumentException("Invalid YearMonth format: '$source' (expected YYYY-MM)", ex)
    }

    override fun addFormatters(registry: FormatterRegistry) {
        registry.addConverter(this)
    }
}
