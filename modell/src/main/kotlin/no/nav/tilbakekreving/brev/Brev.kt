package no.nav.tilbakekreving.brev

import no.nav.tilbakekreving.entities.BrevEntity
import no.nav.tilbakekreving.historikk.Historikk
import java.time.LocalDate
import java.util.UUID

sealed interface Brev : Historikk.HistorikkInnslag<UUID> {
    var journalpostId: String?
    var sendtTid: LocalDate

    fun brevSendt(journalpostId: String)

    fun tilEntity(tilbakekrevingId: String): BrevEntity
}
