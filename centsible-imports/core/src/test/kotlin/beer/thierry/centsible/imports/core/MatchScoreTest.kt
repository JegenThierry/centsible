package beer.thierry.centsible.imports.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MatchScoreTest {

    @Test
    fun `presets are strictly ordered from NO_MATCH to EXACT`() {
        val ordered = listOf(
            MatchScore.NO_MATCH,
            MatchScore.WEAK,
            MatchScore.PARTIAL,
            MatchScore.STRONG,
            MatchScore.EXACT,
        )
        for (i in 1 until ordered.size) {
            assertTrue(
                ordered[i - 1] < ordered[i],
                "${ordered[i - 1].value} should be < ${ordered[i].value}",
            )
        }
    }

    @Test
    fun `compareTo follows underlying int order`() {
        assertEquals(0, MatchScore(50).compareTo(MatchScore(50)))
        assertTrue(MatchScore(10).compareTo(MatchScore(20)) < 0)
        assertTrue(MatchScore(99).compareTo(MatchScore(1)) > 0)
    }
}
