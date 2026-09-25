package no.nav.tilbakekreving.brev

import no.nav.tilbakekreving.entities.BrevEntity
import no.nav.tilbakekreving.historikk.Historikk
import no.nav.tilbakekreving.kontrakter.frontend.models.DokumentInfoDto
import java.time.LocalDate
import java.util.UUID

sealed interface Brev : Historikk.HistorikkInnslag<UUID> {
    var journalpostId: String?
    var dokumentInfoId: String?
    var sendtTid: LocalDate

    fun brevSendt(journalpostId: String, dokumentInfoId: String)

    fun tilEntity(tilbakekrevingId: String): BrevEntity

    fun tilFrontendDto(): DokumentInfoDto? = when (journalpostId) {
        null -> null
        else -> DokumentInfoDto(
            brevSendt = sendtTid,
            journalpostId = journalpostId!!,
            dokumentId = dokumentInfoId!!,
        )
    }
}
