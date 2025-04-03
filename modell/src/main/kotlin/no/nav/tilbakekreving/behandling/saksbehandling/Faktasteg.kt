package no.nav.tilbakekreving.behandling.saksbehandling

import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.api.v1.dto.FaktaFeilutbetalingDto
import no.nav.tilbakekreving.api.v1.dto.FeilutbetalingsperiodeDto
import no.nav.tilbakekreving.api.v1.dto.VurderingAvBrukersUttalelseDto
import no.nav.tilbakekreving.api.v2.Opprettelsevalg
import no.nav.tilbakekreving.brev.BrevHistorikk
import no.nav.tilbakekreving.eksternfagsak.EksternFagsakBehandling
import no.nav.tilbakekreving.hendelse.KravgrunnlagHendelse
import no.nav.tilbakekreving.historikk.HistorikkReferanse
import no.nav.tilbakekreving.kontrakter.Faktainfo
import no.nav.tilbakekreving.kontrakter.Tilbakekrevingsvalg
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.kontrakter.faktaomfeilutbetaling.HarBrukerUttaltSeg
import java.util.UUID

class Faktasteg(
    private val eksternFagsakBehandling: HistorikkReferanse<UUID, EksternFagsakBehandling>,
    private val kravgrunnlag: HistorikkReferanse<UUID, KravgrunnlagHendelse>,
    private val brevHistorikk: BrevHistorikk,
    private val tilbakekreving: Tilbakekreving,
) : Saksbehandlingsteg<FaktaFeilutbetalingDto> {
    override val type: Behandlingssteg = Behandlingssteg.FAKTA

    override fun erFullstending(): Boolean {
        return true
    }

    override fun tilFrontendDto(): FaktaFeilutbetalingDto {
        return FaktaFeilutbetalingDto(
            varsletBeløp = brevHistorikk.sisteVarselbrev()?.varsletBeløp,
            totalFeilutbetaltPeriode = kravgrunnlag.entry.totaltFeilutbetaltPeriode(),
            totaltFeilutbetaltBeløp = kravgrunnlag.entry.totalFeilutbetalBeløpForAllePerioder(),
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
                        when (tilbakekreving.opprettelsesvalg) {
                            Opprettelsevalg.UTSETT_BEHANDLING_MED_VARSEL -> Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_MED_VARSEL
                            Opprettelsevalg.UTSETT_BEHANDLING_UTEN_VARSEL -> Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_UTEN_VARSEL
                            Opprettelsevalg.OPPRETT_BEHANDLING_MED_VARSEL -> Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_AUTOMATISK
                        },
                    konsekvensForYtelser = emptySet(),
                ),
            kravgrunnlagReferanse = kravgrunnlag.entry.referanse,
            vurderingAvBrukersUttalelse =
                VurderingAvBrukersUttalelseDto(
                    HarBrukerUttaltSeg.IKKE_VURDERT,
                    null,
                ),
            opprettetTid = tilbakekreving.opprettet,
        )
    }
    // når kravgrunnlag kommer oppdateres Faktainfo med perioder. Eneste som kommer fra saksbehandler ved opprettelse av tilbakrekreving er:
    // revurderingsresultat
    // revurderingsårsak
    // opprettelsevalg
    // begrunnelseForTilbakekreving
    // revurderingsvedtaksdato

    companion object {
        fun opprett(
            eksternFagsakBehandling: HistorikkReferanse<UUID, EksternFagsakBehandling>,
            kravgrunnlag: HistorikkReferanse<UUID, KravgrunnlagHendelse>,
            brevHistorikk: BrevHistorikk,
            tilbakekreving: Tilbakekreving,
        ): Faktasteg {
            return Faktasteg(
                eksternFagsakBehandling = eksternFagsakBehandling,
                kravgrunnlag = kravgrunnlag,
                brevHistorikk = brevHistorikk,
                tilbakekreving = tilbakekreving,
            )
        }
    }
}
