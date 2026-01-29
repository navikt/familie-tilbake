package no.nav.tilbakekreving.brev

import no.nav.tilbakekreving.FeatureToggles
import no.nav.tilbakekreving.FrontendDto
import no.nav.tilbakekreving.Toggle
import no.nav.tilbakekreving.api.v1.dto.VarselbrevDto
import no.nav.tilbakekreving.entities.BrevEntity
import no.nav.tilbakekreving.entities.Brevtype
import no.nav.tilbakekreving.entities.VarselbrevEntity
import no.nav.tilbakekreving.hendelse.KravgrunnlagHendelse
import no.nav.tilbakekreving.historikk.HistorikkReferanse
import java.time.LocalDate
import java.time.Period
import java.util.UUID

data class Varselbrev(
    override val id: UUID,
    override var journalpostId: String?,
    override var sendtTid: LocalDate,
    val ansvarligSaksbehandlerIdent: String,
    val kravgrunnlag: HistorikkReferanse<UUID, KravgrunnlagHendelse>,
    var fristForUttalelse: LocalDate,
    var tekstFraSaksbehandler: String?,
) : Brev, FrontendDto<VarselbrevDto> {
    fun hentVarsletBeløp(): Long {
        return kravgrunnlag.entry.feilutbetaltBeløpForAllePerioder().toLong()
    }

    override fun brevSendt(journalpostId: String) {
        this.journalpostId = journalpostId
    }

    companion object {
        fun opprett(
            ansvarligSaksbehandlerIdent: String,
            kravgrunnlag: HistorikkReferanse<UUID, KravgrunnlagHendelse>,
            varseltekstFraSaksbehandler: String,
            features: FeatureToggles,
        ): Varselbrev {
            val sendtTid = LocalDate.now()
            val frist = if (features[Toggle.FjernUttalelsesfrist]) {
                Period.ZERO
            } else {
                Period.ofWeeks(3)
            }
            return Varselbrev(
                id = UUID.randomUUID(),
                journalpostId = null,
                sendtTid = sendtTid,
                ansvarligSaksbehandlerIdent = ansvarligSaksbehandlerIdent,
                kravgrunnlag = kravgrunnlag,
                fristForUttalelse = sendtTid.plus(frist),
                tekstFraSaksbehandler = varseltekstFraSaksbehandler,
            )
        }
    }

    override fun tilEntity(tilbakekrevingId: String): BrevEntity {
        return BrevEntity(
            id = id,
            tilbakekrevingRef = tilbakekrevingId,
            brevType = Brevtype.VARSEL_BREV,
            kravgrunnlagRef = kravgrunnlag.tilEntity(),
            varselbrevEntity = VarselbrevEntity(
                brevRef = id,
                journalpostId = journalpostId,
                sendtTid = sendtTid,
                ansvarligSaksbehandlerIdent = ansvarligSaksbehandlerIdent,
                fristForUttalelse = fristForUttalelse,
                tekstFraSaksbehandler = tekstFraSaksbehandler,
            ),
        )
    }

    override fun tilFrontendDto(): VarselbrevDto {
        return VarselbrevDto(varselbrevSendtTid = sendtTid, opprinneligFristForUttalelse = fristForUttalelse, tekstFraSaksbehandler = tekstFraSaksbehandler)
    }
}
