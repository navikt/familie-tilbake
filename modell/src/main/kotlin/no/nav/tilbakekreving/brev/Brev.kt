package no.nav.tilbakekreving.brev

import no.nav.tilbakekreving.entities.BrevEntity
import no.nav.tilbakekreving.historikk.Historikk
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

sealed interface Brev : Historikk.HistorikkInnslag<UUID> {
    var journalpostId: String?
    val opprettetDato: LocalDate
    var sendt: LocalDateTime?

    fun tilEntity(): BrevEntity
}
