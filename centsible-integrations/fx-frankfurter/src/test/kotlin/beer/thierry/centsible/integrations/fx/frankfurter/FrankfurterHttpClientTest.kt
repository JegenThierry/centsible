package beer.thierry.centsible.integrations.fx.frankfurter

import beer.thierry.centsible.api.model.budgetaccount.Currency
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import java.math.BigDecimal
import java.time.LocalDate

class FrankfurterHttpClientTest {

    private val apiBase = "https://api.frankfurter.dev"

    @Test
    fun `fetchRate parses the rate and the effective business-day date from the v2 array response`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val client = FrankfurterHttpClient(builder.build(), apiBase)

        server.expect(requestTo("$apiBase/v2/rates?date=2024-01-02&base=USD&quotes=EUR"))
            .andRespond(
                withSuccess(
                    """[{"date":"2023-12-29","base":"USD","quote":"EUR","rate":0.90717}]""",
                    MediaType.APPLICATION_JSON,
                ),
            )

        val rate = client.fetchRate(Currency.USD, Currency.EUR, LocalDate.of(2024, 1, 2))

        assertEquals(BigDecimal("0.90717"), rate.rate)
        assertEquals(LocalDate.of(2023, 12, 29), rate.rateDate)
        server.verify()
    }

    @Test
    fun `fetchRate throws when the response array contains no rate`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val client = FrankfurterHttpClient(builder.build(), apiBase)

        server.expect(requestTo("$apiBase/v2/rates?date=2024-01-02&base=USD&quotes=EUR"))
            .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON))

        assertThrows(IllegalStateException::class.java) {
            client.fetchRate(Currency.USD, Currency.EUR, LocalDate.of(2024, 1, 2))
        }
    }
}
