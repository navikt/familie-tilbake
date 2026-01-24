package no.nav.tilbakekreving.entities

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.tilbakekreving.brev.Brev
import no.nav.tilbakekreving.brev.Varselbrev
import no.nav.tilbakekreving.feil.Sporing
import no.nav.tilbakekreving.kravgrunnlag.KravgrunnlagHistorikk
import java.time.LocalDate
import java.time.Period
import java.util.UUID

@JsonIgnoreProperties(ignoreUnknown = true)
data class BrevEntity(
    val id: UUID,
    val tilbakekrevingRef: String? = null, // Todo nullable må fjernes etter prod.
    val brevType: Brevtype,
    val kravgrunnlagRef: HistorikkReferanseEntity<UUID>,
    val varselbrevEntity: VarselbrevEntity?,
    // Todo Alle val under må fjernes etter deploy og kjørt migreringen
    @param:JsonAlias("fristForTilbakemelding", "fristForUttalelse")
    val fristForUttalelse: LocalDate? = null,
    val tekstFraSaksbehandler: String? = null,
    val journalpostId: String? = null,
    @param:JsonAlias("sendt", "sendtTid")
    val sendtTid: LocalDate? = null,
    val ansvarligSaksbehandlerIdent: String?,
) {
    fun fraEntity(kravgrunnlagHistorikk: KravgrunnlagHistorikk): Brev {
        val sporing = Sporing("Ukjent", id.toString())
        val sendtTid = sendtTid ?: LocalDate.now()
        return when (brevType) {
            Brevtype.VARSEL_BREV -> {
                varselbrevEntity?.fraEntity(
                    id,
                    kravgrunnlagHistorikk.finn(kravgrunnlagRef.id, sporing),
                ) ?: Varselbrev(
                    id = id,
                    journalpostId = journalpostId,
                    sendtTid = sendtTid,
                    ansvarligSaksbehandlerIdent = ansvarligSaksbehandlerIdent!!,
                    kravgrunnlag = kravgrunnlagHistorikk.finn(kravgrunnlagRef.id, sporing),
                    fristForUttalelse = fristForUttalelse ?: sendtTid.plus(Period.ofWeeks(3)),
                    tekstFraSaksbehandler = tekstFraSaksbehandler,
                )
            }
        }
    }
}

enum class Brevtype {
    VARSEL_BREV,
}
