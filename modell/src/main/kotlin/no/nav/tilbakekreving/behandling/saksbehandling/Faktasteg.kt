package no.nav.tilbakekreving.behandling.saksbehandling

import no.nav.tilbakekreving.api.v1.dto.FaktaFeilutbetalingDto
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
import no.nav.tilbakekreving.kontrakter.faktaomfeilutbetaling.Hendelsestype
import no.nav.tilbakekreving.kontrakter.faktaomfeilutbetaling.Hendelsesundertype
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import java.time.LocalDateTime
import java.util.UUID

class Faktasteg(
    private val eksternFagsakBehandling: HistorikkReferanse<UUID, EksternFagsakBehandling>,
    private val kravgrunnlag: HistorikkReferanse<UUID, KravgrunnlagHendelse>,
    private val brevHistorikk: BrevHistorikk,
    private val tilbakekrevingOpprettet: LocalDateTime,
    private val opprettelsesvalg: Opprettelsesvalg,
    private var vurdering: Vurdering,
) : Saksbehandlingsteg<FaktaFeilutbetalingDto> {
    override val type: Behandlingssteg = Behandlingssteg.FAKTA

    override fun erFullstending(): Boolean {
        return true // vurdering.erFullstendig()
    }

    internal fun vurder(vurdering: Vurdering) {
        this.vurdering = vurdering
    }

    override fun tilFrontendDto(): FaktaFeilutbetalingDto {
        return FaktaFeilutbetalingDto(
            varsletBeløp = brevHistorikk.sisteVarselbrev()?.varsletBeløp,
            totalFeilutbetaltPeriode = kravgrunnlag.entry.totaltFeilutbetaltPeriode(),
            totaltFeilutbetaltBeløp = kravgrunnlag.entry.feilutbetaltBeløpForAllePerioder(),
            feilutbetaltePerioder = vurdering.perioder.map {
                FeilutbetalingsperiodeDto(
                    periode = it.periode,
                    feilutbetaltBeløp = kravgrunnlag.entry.totaltBeløpFor(it.periode),
                    hendelsestype = it.hendelsestype,
                    hendelsesundertype = it.hendelsesundertype,
                )
            },
            revurderingsvedtaksdato = eksternFagsakBehandling.entry.revurderingsvedtaksdato,
            begrunnelse = vurdering.årsak,
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
            vurderingAvBrukersUttalelse = VurderingAvBrukersUttalelseDto(
                harBrukerUttaltSeg = when (vurdering.uttalelse) {
                    is Uttalelse.Ja -> HarBrukerUttaltSeg.JA
                    is Uttalelse.Nei -> HarBrukerUttaltSeg.NEI
                    is Uttalelse.IkkeAktuelt -> HarBrukerUttaltSeg.IKKE_AKTUELT
                    is Uttalelse.IkkeVurdert -> HarBrukerUttaltSeg.IKKE_VURDERT
                },
                beskrivelse = (vurdering.uttalelse as? Uttalelse.Ja)?.begrunnelse,
            ),
            opprettetTid = tilbakekrevingOpprettet,
        )
    }

    fun tilEntity(): FaktastegEntity {
        return FaktastegEntity(
            tilbakekrevingOpprettet = tilbakekrevingOpprettet,
            opprettelsesvalg = opprettelsesvalg,
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
                vurdering = Vurdering(
                    perioder = kravgrunnlag.entry.perioder.map {
                        FaktaPeriode(
                            periode = it.periode,
                            hendelsestype = Hendelsestype.ANNET,
                            hendelsesundertype = Hendelsesundertype.ANNET_FRITEKST,
                        )
                    },
                    årsak = eksternFagsakBehandling.entry.begrunnelseForTilbakekreving,
                    uttalelse = Uttalelse.IkkeVurdert,
                ),
            )
        }
    }

    class Vurdering(
        val perioder: List<FaktaPeriode>,
        val årsak: String,
        val uttalelse: Uttalelse,
    )

    class FaktaPeriode(
        val periode: Datoperiode,
        val hendelsestype: Hendelsestype,
        val hendelsesundertype: Hendelsesundertype,
    )

    sealed interface Uttalelse {
        class Ja(val begrunnelse: String) : Uttalelse

        data object Nei : Uttalelse

        data object IkkeAktuelt : Uttalelse

        data object IkkeVurdert : Uttalelse
    }
}
