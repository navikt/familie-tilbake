package no.nav.tilbakekreving.entities

import com.fasterxml.jackson.annotation.JsonAlias
import no.nav.tilbakekreving.brev.Varselbrev
import no.nav.tilbakekreving.hendelse.KravgrunnlagHendelse
import no.nav.tilbakekreving.historikk.HistorikkReferanse
import java.time.LocalDate
import java.time.Period
import java.util.UUID

data class VarselbrevEntity(
    val id: UUID = UUID.randomUUID(),
    val brevRef: UUID,
    val journalpostId: String?,
    @param:JsonAlias("sendt", "sendtTid")
    val sendtTid: LocalDate? = null,
    val ansvarligSaksbehandlerIdent: String,
    @param:JsonAlias("fristForTilbakemelding", "fristForUttalelse")
    val fristForUttalelse: LocalDate?,
    val tekstFraSaksbehandler: String?,
) {
    fun fraEntity(id: UUID, kravgrunnlagHistorikk: HistorikkReferanse<UUID, KravgrunnlagHendelse>): Varselbrev {
        val sendtTid = sendtTid ?: LocalDate.now()
        return Varselbrev(
            id = id,
            journalpostId = journalpostId,
            sendtTid = sendtTid,
            ansvarligSaksbehandlerIdent = ansvarligSaksbehandlerIdent,
            kravgrunnlag = kravgrunnlagHistorikk,
            fristForUttalelse = fristForUttalelse ?: sendtTid.plus(Period.ofWeeks(3)),
            tekstFraSaksbehandler = tekstFraSaksbehandler,
        )
    }
}
