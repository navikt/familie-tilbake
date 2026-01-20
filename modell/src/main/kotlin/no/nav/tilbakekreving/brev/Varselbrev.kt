package no.nav.tilbakekreving.brev

import no.nav.tilbakekreving.FrontendDto
import no.nav.tilbakekreving.api.v1.dto.VarselbrevDto
import no.nav.tilbakekreving.behandling.saksbehandling.RegistrertBrevmottaker
import no.nav.tilbakekreving.entities.BrevEntity
import no.nav.tilbakekreving.entities.Brevtype
import no.nav.tilbakekreving.hendelse.KravgrunnlagHendelse
import no.nav.tilbakekreving.historikk.HistorikkReferanse
import java.time.LocalDate
import java.time.Period
import java.util.UUID

data class Varselbrev(
    override val id: UUID,
    override val opprettetDato: LocalDate,
    override var journalpostId: String?,
    override var sendtTid: LocalDate,
    val mottaker: RegistrertBrevmottaker,
    val brevmottakerStegId: UUID?,
    val ansvarligSaksbehandlerIdent: String?,
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
            mottaker: RegistrertBrevmottaker,
            brevmottakerStegId: UUID,
            ansvarligSaksbehandlerIdent: String,
            kravgrunnlag: HistorikkReferanse<UUID, KravgrunnlagHendelse>,
            varseltekstFraSaksbehandler: String,
        ): Varselbrev {
            val sendtTid = LocalDate.now()
            return Varselbrev(
                id = UUID.randomUUID(),
                opprettetDato = LocalDate.now(),
                journalpostId = null,
                sendtTid = sendtTid,
                mottaker = mottaker,
                brevmottakerStegId = brevmottakerStegId,
                ansvarligSaksbehandlerIdent = ansvarligSaksbehandlerIdent,
                kravgrunnlag = kravgrunnlag,
                fristForUttalelse = sendtTid.plus(Period.ofWeeks(3)),
                tekstFraSaksbehandler = varseltekstFraSaksbehandler,
            )
        }
    }

    override fun tilEntity(): BrevEntity {
        return BrevEntity(
            brevType = Brevtype.VARSEL_BREV,
            id = id,
            brevmottakerStegRef = brevmottakerStegId,
            opprettetDato = opprettetDato,
            journalpostId = journalpostId,
            sendtTid = sendtTid,
            mottaker = mottaker.tilEntity(brevmottakerStegId, null),
            ansvarligSaksbehandlerIdent = ansvarligSaksbehandlerIdent,
            kravgrunnlagRef = kravgrunnlag.tilEntity(),
            fristForUttalelse = fristForUttalelse,
            tekstFraSaksbehandler = tekstFraSaksbehandler,
        )
    }

    override fun tilFrontendDto(): VarselbrevDto {
        return VarselbrevDto(varselbrevSendtTid = sendtTid, opprinneligFristForUttalelse = fristForUttalelse, tekstFraSaksbehandler = tekstFraSaksbehandler)
    }
}
