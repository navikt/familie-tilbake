package no.nav.tilbakekreving.brev

import no.nav.tilbakekreving.behandling.saksbehandling.RegistrertBrevmottaker
import no.nav.tilbakekreving.entities.BrevEntity
import no.nav.tilbakekreving.entities.Brevtype
import no.nav.tilbakekreving.hendelse.KravgrunnlagHendelse
import no.nav.tilbakekreving.historikk.HistorikkReferanse
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Period
import java.util.UUID

data class Varselbrev(
    override val id: UUID,
    override val opprettetDato: LocalDate,
    override var journalpostId: String?,
    override var sendt: LocalDateTime?,
    val mottaker: RegistrertBrevmottaker,
    val brevmottakerStegId: UUID?,
    val ansvarligSaksbehandlerIdent: String?,
    val kravgrunnlag: HistorikkReferanse<UUID, KravgrunnlagHendelse>,
    val fristForTilbakemelding: LocalDate,
) : Brev {
    fun hentVarsletBeløp(): Long {
        return kravgrunnlag.entry.feilutbetaltBeløpForAllePerioder().toLong()
    }

    companion object {
        fun opprett(
            mottaker: RegistrertBrevmottaker,
            brevmottakerStegId: UUID,
            ansvarligSaksbehandlerIdent: String,
            kravgrunnlag: HistorikkReferanse<UUID, KravgrunnlagHendelse>,
        ): Brev {
            return Varselbrev(
                id = UUID.randomUUID(),
                opprettetDato = LocalDate.now(),
                journalpostId = null,
                sendt = null,
                mottaker = mottaker,
                brevmottakerStegId = brevmottakerStegId,
                ansvarligSaksbehandlerIdent = ansvarligSaksbehandlerIdent,
                kravgrunnlag = kravgrunnlag,
                fristForTilbakemelding = LocalDate.now().plus(Period.ofWeeks(3)),
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
            sendt = sendt,
            mottaker = mottaker.tilEntity(brevmottakerStegId, null),
            ansvarligSaksbehandlerIdent = ansvarligSaksbehandlerIdent,
            kravgrunnlagRef = kravgrunnlag.tilEntity(),
            fristForTilbakemelding = fristForTilbakemelding,
        )
    }
}
