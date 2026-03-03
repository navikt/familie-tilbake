package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.brev.Varselbrev
import no.nav.tilbakekreving.feil.Sporing
import no.nav.tilbakekreving.kravgrunnlag.KravgrunnlagHistorikk
import java.time.LocalDate
import java.time.Period
import java.util.UUID

data class VarselbrevEntity(
    val id: UUID = UUID.randomUUID(),
    val brevRef: UUID,
    val kravgrunnlagRef: HistorikkReferanseEntity<UUID>,
    val journalpostId: String?,
    val sendtTid: LocalDate? = null,
    val ansvarligSaksbehandlerIdent: String,
    val fristForUttalelse: LocalDate?,
    val tekstFraSaksbehandler: String?,
) {
    fun fraEntity(id: UUID, kravgrunnlagHistorikk: KravgrunnlagHistorikk): Varselbrev {
        val sendtTid = sendtTid ?: LocalDate.now()
        val sporing = Sporing("Ukjent", id.toString())
        return Varselbrev(
            id = id,
            journalpostId = journalpostId,
            sendtTid = sendtTid,
            ansvarligSaksbehandlerIdent = ansvarligSaksbehandlerIdent,
            kravgrunnlag = kravgrunnlagHistorikk.finn(kravgrunnlagRef.id, sporing),
            fristForUttalelse = fristForUttalelse ?: sendtTid.plus(Period.ofWeeks(3)),
            tekstFraSaksbehandler = tekstFraSaksbehandler,
        )
    }
}
