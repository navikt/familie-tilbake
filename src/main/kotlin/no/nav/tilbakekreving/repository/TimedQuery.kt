package no.nav.tilbakekreving.repository

import io.micrometer.core.instrument.Metrics
import io.micrometer.core.instrument.Timer
import org.springframework.jdbc.core.JdbcTemplate
import java.sql.ResultSet
import java.time.Duration
import java.util.concurrent.TimeUnit

inline fun <T> JdbcTemplate.timedQuery(
    name: String,
    sql: String,
    vararg args: Any?,
    crossinline mapper: (ResultSet, Int) -> T,
): List<T> {
    var mappingNanos = 0L
    val start = System.nanoTime()
    try {
        return query(
            sql,
            { rs, row ->
                val mapStart = System.nanoTime()
                try {
                    mapper(rs, row)
                } finally {
                    mappingNanos += System.nanoTime() - mapStart
                }
            },
            *args,
        )
    } finally {
        queryTimer(name).record(System.nanoTime() - start - mappingNanos, TimeUnit.NANOSECONDS)
    }
}

@PublishedApi
internal fun queryTimer(name: String): Timer = Timer.builder("tilbakekreving.db.query")
    .tag("query", name)
    .publishPercentileHistogram()
    .minimumExpectedValue(Duration.ofMillis(1))
    .maximumExpectedValue(Duration.ofSeconds(10))
    .register(Metrics.globalRegistry)
