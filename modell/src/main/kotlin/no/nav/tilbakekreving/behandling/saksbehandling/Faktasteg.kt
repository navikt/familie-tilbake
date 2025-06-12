package no.nav.tilbakekreving.behandling.saksbehandling

import no.nav.tilbakekreving.api.v1.dto.FaktaFeilutbetalingDto
import no.nav.tilbakekreving.api.v1.dto.FaktaFeilutbetalingsperiodeDto
import no.nav.tilbakekreving.api.v1.dto.FeilutbetalingsperiodeDto
import no.nav.tilbakekreving.api.v1.dto.VurderingAvBrukersUttalelseDto
import no.nav.tilbakekreving.api.v2.Opprettelsesvalg
import no.nav.tilbakekreving.brev.BrevHistorikk
import no.nav.tilbakekreving.eksternfagsak.EksternFagsakBehandling
import no.nav.tilbakekreving.entities.FaktastegEntity
import no.nav.tilbakekreving.hendelse.KravgrunnlagHendelse
import no.nav.tilbakekreving.historikk.HistorikkReferanse
import no.nav.tilbakekreving.kontrakter.Faktainfo
import no.nav.tilbakekreving.kontrakter.Tilbakekrevingsvalg
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.kontrakter.faktaomfeilutbetaling.HarBrukerUttaltSeg
import java.time.LocalDateTime
import java.util.UUID

class Faktasteg(
    private val eksternFagsakBehandling: HistorikkReferanse<UUID, EksternFagsakBehandling>,
    private val kravgrunnlag: HistorikkReferanse<UUID, KravgrunnlagHendelse>,
    private val brevHistorikk: BrevHistorikk,
    private val tilbakekrevingOpprettet: LocalDateTime,
    private val opprettelsesvalg: Opprettelsesvalg,
) : Saksbehandlingsteg<FaktaFeilutbetalingDto> {
    override val type: Behandlingssteg = Behandlingssteg.FAKTA

    override fun erFullstending(): Boolean {
        return true
    }

    internal fun behandleFakta(fakta: FaktaFeilutbetalingsperiodeDto) {
        // TODO
    }

    override fun tilFrontendDto(): FaktaFeilutbetalingDto {
        return FaktaFeilutbetalingDto(
            varsletBeløp = brevHistorikk.sisteVarselbrev()?.varsletBeløp,
            totalFeilutbetaltPeriode = kravgrunnlag.entry.totaltFeilutbetaltPeriode(),
            totaltFeilutbetaltBeløp = kravgrunnlag.entry.feilutbetaltBeløpForAllePerioder(),
            feilutbetaltePerioder =
                kravgrunnlag.entry.datoperioder().map {
                    FeilutbetalingsperiodeDto(
                        periode = it,
                        feilutbetaltBeløp = kravgrunnlag.entry.totaltBeløpFor(it),
                        hendelsestype = null,
                        hendelsesundertype = null,
                    )
                },
            revurderingsvedtaksdato = eksternFagsakBehandling.entry.revurderingsvedtaksdato,
            begrunnelse = eksternFagsakBehandling.entry.begrunnelseForTilbakekreving,
            faktainfo =
                Faktainfo(
                    revurderingsårsak = eksternFagsakBehandling.entry.revurderingsårsak,
                    revurderingsresultat = eksternFagsakBehandling.entry.revurderingsresultat,
                    tilbakekrevingsvalg =
                        when (opprettelsesvalg) {
                            Opprettelsesvalg.UTSETT_BEHANDLING_MED_VARSEL -> Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_MED_VARSEL
                            Opprettelsesvalg.UTSETT_BEHANDLING_UTEN_VARSEL -> Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_UTEN_VARSEL
                            Opprettelsesvalg.OPPRETT_BEHANDLING_MED_VARSEL -> Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_AUTOMATISK
                        },
                    konsekvensForYtelser = emptySet(),
                ),
            kravgrunnlagReferanse = kravgrunnlag.entry.referanse,
            vurderingAvBrukersUttalelse =
                VurderingAvBrukersUttalelseDto(
                    HarBrukerUttaltSeg.IKKE_VURDERT,
                    null,
                ),
            opprettetTid = tilbakekrevingOpprettet,
        )
    }

    fun tilEntity(): FaktastegEntity {
        return FaktastegEntity(
            eksternFagsakBehandlingRef = eksternFagsakBehandling.entry.internId.toString(),
            kravgrunnlagRef = kravgrunnlag.entry.internId.toString(),
            brevHistorikk = brevHistorikk.tilEntity(),
            tilbakekrevingOpprettet = tilbakekrevingOpprettet.toString(),
            opprettelsesvalg = opprettelsesvalg.name,
        )
    }

    companion object {
        fun opprett(
            eksternFagsakBehandling: HistorikkReferanse<UUID, EksternFagsakBehandling>,
            kravgrunnlag: HistorikkReferanse<UUID, KravgrunnlagHendelse>,
            brevHistorikk: BrevHistorikk,
            tilbakekrevingOpprettet: LocalDateTime,
            opprettelsesvalg: Opprettelsesvalg,
        ): Faktasteg {
            return Faktasteg(
                eksternFagsakBehandling = eksternFagsakBehandling,
                kravgrunnlag = kravgrunnlag,
                brevHistorikk = brevHistorikk,
                tilbakekrevingOpprettet = tilbakekrevingOpprettet,
                opprettelsesvalg = opprettelsesvalg,
            )
        }
    }

    sealed interface Vurdering {
        val uttalelse: Uttalelse

        // val feilutbetalinger: List<Feil>
        val hendelsestype: String
        val hendelsesundertype: String
    }

    sealed interface Uttalelse {
        class Ja(val begrunnelse: String) : Uttalelse

        data object Nei : Uttalelse

        data object IkkeAktuelt : Uttalelse

        data object IkkeVurdert : Uttalelse
    }
}
