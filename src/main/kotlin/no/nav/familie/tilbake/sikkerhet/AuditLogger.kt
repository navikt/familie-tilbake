package no.nav.familie.tilbake.sikkerhet

/**
 * [custom1], [custom2], [custom3] brukes for å logge ekstra felter, eks fagsak, behandling,
 * disse logges til cs3,cs5,cs6 då cs1,cs2 og cs4 er til internt bruk
 * Kan brukes med eks CustomKeyValue(key=fagsak, value=fagsakId)
 */
data class Sporingsdata(
    val event: AuditLoggerEvent,
    val personIdent: String,
    val custom1: CustomKeyValue? = null,
    val custom2: CustomKeyValue? = null,
    val custom3: CustomKeyValue? = null,
)

enum class AuditLoggerEvent(
    val type: String,
) {
    CREATE("create"),
    UPDATE("update"),
    ACCESS("access"),
    NONE("Not logged"),
}

data class CustomKeyValue(
    val key: String,
    val value: String,
)

interface AuditLogger {
    fun log(data: Sporingsdata)
}
