package no.nav.tilbakekreving.entities

import com.fasterxml.jackson.annotation.JsonAlias
import no.nav.tilbakekreving.brev.Brev
import no.nav.tilbakekreving.brev.Varselbrev
import no.nav.tilbakekreving.feil.Sporing
import no.nav.tilbakekreving.kravgrunnlag.KravgrunnlagHistorikk
import java.time.LocalDate
import java.time.Period
import java.util.UUID

data class BrevEntity(
    val brevType: Brevtype,
    val id: UUID,
    val brevmottakerStegRef: UUID? = null, // Todo nullable må fjernes etter prod.
    val opprettetDato: LocalDate,
    val journalpostId: String?,
    @param:JsonAlias("sendt", "sendtTid")
    val sendtTid: LocalDate? = null,
    val mottaker: RegistrertBrevmottakerEntity,
    val ansvarligSaksbehandlerIdent: String?,
    val kravgrunnlagRef: HistorikkReferanseEntity<UUID>,
    @param:JsonAlias("fristForTilbakemelding", "fristForUttalelse")
    val fristForUttalelse: LocalDate?,
    val tekstFraSaksbehandler: String?,
) {
    fun fraEntity(kravgrunnlagHistorikk: KravgrunnlagHistorikk): Brev {
        val sporing = Sporing("Ukjent", id.toString())
        val sendtTid = sendtTid ?: opprettetDato
        return when (brevType) {
            Brevtype.VARSEL_BREV -> Varselbrev(
                id = requireNotNull(id) { "Id kreves for Brev" },
                brevmottakerStegId = brevmottakerStegRef,
                opprettetDato = requireNotNull(opprettetDato) { "opprettetDato kreves for Brev" },
                journalpostId = journalpostId,
                sendtTid = sendtTid,
                mottaker = mottaker.fraEntity(),
                ansvarligSaksbehandlerIdent = ansvarligSaksbehandlerIdent,
                kravgrunnlag = kravgrunnlagHistorikk.finn(kravgrunnlagRef.id, sporing),
                fristForUttalelse = fristForUttalelse ?: sendtTid.plus(Period.ofWeeks(3)),
                tekstFraSaksbehandler = tekstFraSaksbehandler,
            )
        }
    }
}

enum class Brevtype {
    VARSEL_BREV,
}
