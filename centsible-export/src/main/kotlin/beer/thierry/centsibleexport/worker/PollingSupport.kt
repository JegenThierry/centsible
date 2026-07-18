package beer.thierry.centsibleexport.worker

import org.slf4j.Logger

/**
 * Runs [claim] and returns its result, logging and swallowing any exception so the scheduled
 * poll loop never propagates failures out of [org.springframework.scheduling.annotation.Scheduled].
 * Returns `null` either when nothing is claimable or when the claim itself failed.
 */
internal inline fun <T> Logger.claimOrLog(failureMessage: String, claim: () -> T?): T? =
    try {
        claim()
    } catch (ex: Exception) {
        error(failureMessage, ex)
        null
    }

internal fun Throwable.failureReason(): String =
    message ?: this::class.qualifiedName ?: "unknown error"

/** Milliseconds elapsed since a [System.nanoTime] reading, for timing log fields. */
internal fun elapsedMsSince(startNanos: Long): Long = (System.nanoTime() - startNanos) / 1_000_000
