package no.nav.familie.tilbake.historikkinnslag

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

data class Historikkinnslag(
    @Id val id: UUID = UUID.randomUUID(),
    val behandlingId: UUID,
    val type: Historikkinnslagstype,
    @Column("aktor")
    val aktør: Aktør,
    val tittel: String,
    val tekst: String? = null,
    val steg: String? = null,
    val journalpostId: String? = null,
    val dokumentId: String? = null,
    val opprettetAv: String,
    val opprettetTid: LocalDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS),
)

enum class Aktør {
    SAKSBEHANDLER,

    BESLUTTER,

    VEDTAKSLØSNING,
}

enum class Historikkinnslagstype {
    HENDELSE,

    SKJERMLENKE,

    BREV,
}
